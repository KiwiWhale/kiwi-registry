package io.github.kiwiwhale.kiwiregistry.bootstrap

import io.github.kiwiwhale.kiwiregistry.core.Registry
import io.github.kiwiwhale.kiwiregistry.core.RegistryGraph
import io.github.kiwiwhale.kiwiregistry.core.RegistryKey
import io.github.kiwiwhale.kiwiregistry.exception.BootstrapAlreadyCompletedException
import io.github.kiwiwhale.kiwiregistry.exception.BootstrapInProgressException
import io.github.kiwiwhale.kiwiregistry.exception.BootstrapNotCompletedException
import io.github.kiwiwhale.kiwiregistry.exception.DuplicateContributionException
import io.github.kiwiwhale.kiwiregistry.exception.InvalidContributionException
import io.github.kiwiwhale.kiwiregistry.registry.MutableRegistry

internal class RegistryBootstrap {

    private data class HolderSnapshot(
        val holder: RegistryHolder,
        val registries: Map<RegistryKey<*, *>, Map<*, *>>
    )

    private val contributions = ArrayList<Contribution>()
    private var snapshots = emptyList<HolderSnapshot>()

    @Volatile
    private var currentGraph: RegistryGraph? = null

    @Volatile
    private var state = BootstrapState.CREATED

    val isInitialized: Boolean
        get() = state == BootstrapState.COMPLETED

    @Synchronized
    fun submit(contribution: Contribution) {
        when (state) {
            BootstrapState.CREATED -> Unit
            BootstrapState.BUILDING -> throw BootstrapInProgressException()
            BootstrapState.COMPLETED -> throw BootstrapAlreadyCompletedException()
        }

        if (contributions.any { it.holder === contribution.holder }) {
            throw DuplicateContributionException(
                contribution.holder::class.qualifiedName
                    ?: contribution.holder::class.simpleName
                    ?: "Unknown"
            )
        }

        contributions += contribution
    }

    @Synchronized
    fun build() {
        when (state) {
            BootstrapState.CREATED -> Unit
            BootstrapState.BUILDING -> throw BootstrapInProgressException()
            BootstrapState.COMPLETED -> throw BootstrapAlreadyCompletedException()
        }

        state = BootstrapState.BUILDING

        val newSnapshots: List<HolderSnapshot>
        val newGraph: RegistryGraph

        try {
            contributions.forEach { contribution ->
                contribution.preHooks.forEach { it() }
            }

            newSnapshots = contributions.map(::evaluate)
            newGraph = createGraph(newSnapshots)

        } catch (exception: Throwable) {
            state = BootstrapState.CREATED
            throw exception
        }

        snapshots = newSnapshots
        currentGraph = newGraph
        state = BootstrapState.COMPLETED

        runPostHooks(contributions)
    }

    @Synchronized
    fun rebuildRegistries(holder: RegistryHolder) {
        when (state) {
            BootstrapState.CREATED -> throw BootstrapNotCompletedException()
            BootstrapState.BUILDING -> throw BootstrapInProgressException()
            BootstrapState.COMPLETED -> Unit
        }

        val index = snapshots.indexOfFirst {
            it.holder === holder
        }

        if (index == -1) {
            throw InvalidContributionException(
                "No contribution found for holder: " +
                        (holder::class.qualifiedName ?: "Unknown")
            )
        }

        val contribution = contributions[index]
        contribution.preHooks.forEach { it() }

        val updatedSnapshot = evaluate(contribution)

        val updatedSnapshots = snapshots.toMutableList().apply {
            set(index, updatedSnapshot)
        }

        val updatedGraph = createGraph(updatedSnapshots)

        snapshots = updatedSnapshots
        currentGraph = updatedGraph

        runPostHooks(listOf(contribution))
    }

    fun graph(): RegistryGraph =
        currentGraph ?: throw BootstrapNotCompletedException()

    fun <K, V> getRegistry(
        key: RegistryKey<K, V>
    ): Registry<K, V> =
        graph().getRegistry(key)

    private fun runPostHooks(contributions: List<Contribution>) {
        contributions.forEach { contribution ->
            contribution.postHooks.forEach { it() }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun evaluate(
        contribution: Contribution
    ): HolderSnapshot {
        val registries =
            LinkedHashMap<RegistryKey<*, *>, Map<*, *>>()

        for ((key, builder) in contribution.registries) {
            val typedKey = key as RegistryKey<Any?, Any?>

            val typedBuilder = builder as RegistryContributionBuilder<Any?, Any?>.() -> Unit

            val registryBuilder = RegistryContributionBuilder(typedKey)
            registryBuilder.typedBuilder()

            registries[key] = registryBuilder.entries.toMap()
        }

        return HolderSnapshot(contribution.holder, registries.toMap())
    }

    @Suppress("UNCHECKED_CAST")
    private fun createGraph(
        snapshots: List<HolderSnapshot>
    ): RegistryGraph {
        val keys = LinkedHashSet<RegistryKey<*, *>>()

        snapshots.forEach { snapshot ->
            keys.addAll(snapshot.registries.keys)
        }

        val registries = LinkedHashMap<RegistryKey<*, *>, Registry<*, *>>()

        for (key in keys) {
            val typedKey = key as RegistryKey<Any?, Any?>

            val mutableRegistry = MutableRegistry(typedKey)

            for ((holder, holderRegistries) in snapshots) {
                val entries = holderRegistries[key]
                    ?: continue

                entries.forEach { (entryKey, value) ->
                    mutableRegistry.register(
                        holder,
                        entryKey,
                        value
                    )
                }
            }

            registries[key] = mutableRegistry.freeze()
        }

        return RegistryGraph(registries)
    }
}
