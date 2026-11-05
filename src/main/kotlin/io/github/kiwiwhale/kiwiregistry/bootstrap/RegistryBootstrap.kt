package io.github.kiwiwhale.kiwiregistry.bootstrap

import io.github.kiwiwhale.kiwiregistry.core.Registry
import io.github.kiwiwhale.kiwiregistry.core.RegistryGraph
import io.github.kiwiwhale.kiwiregistry.core.RegistryKey
import io.github.kiwiwhale.kiwiregistry.exception.BootstrapAlreadyCompletedException
import io.github.kiwiwhale.kiwiregistry.exception.BootstrapDisposedException
import io.github.kiwiwhale.kiwiregistry.exception.BootstrapInProgressException
import io.github.kiwiwhale.kiwiregistry.exception.BootstrapNotCompletedException
import io.github.kiwiwhale.kiwiregistry.exception.DuplicateContributionException
import io.github.kiwiwhale.kiwiregistry.exception.InvalidContributionException
import io.github.kiwiwhale.kiwiregistry.exception.PostHookExecutionException
import io.github.kiwiwhale.kiwiregistry.registry.MutableRegistry
import java.util.Collections

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

    val isDisposed: Boolean
        get() = state == BootstrapState.DISPOSED

    private fun checkNotCompleted() {
        when (state) {
            BootstrapState.CREATED -> Unit
            BootstrapState.BUILDING -> throw BootstrapInProgressException()
            BootstrapState.COMPLETED -> throw BootstrapAlreadyCompletedException()
            BootstrapState.DISPOSED -> throw BootstrapDisposedException()
        }
    }

    private fun checkCompleted() {
        when (state) {
            BootstrapState.CREATED -> throw BootstrapNotCompletedException()
            BootstrapState.BUILDING -> throw BootstrapInProgressException()
            BootstrapState.COMPLETED -> Unit
            BootstrapState.DISPOSED -> throw BootstrapDisposedException()
        }
    }

    @Synchronized
    fun submit(contribution: Contribution) {
        checkNotCompleted()

        if (contributions.any { it.holder === contribution.holder }) {
            throw DuplicateContributionException(
                contribution.holder::class.qualifiedName
                    ?: contribution.holder::class.simpleName
                    ?: "Unknown"
            )
        }

        contributions.add(contribution)
    }

    @Synchronized
    fun build() {
        checkNotCompleted()

        state = BootstrapState.BUILDING

        try {
            val pendingContributions = contributions.toList()

            executePreHooks(pendingContributions)

            val newSnapshots = pendingContributions.map(::evaluate)

            val newGraph = createGraph(newSnapshots)

            snapshots = newSnapshots
            currentGraph = newGraph
            state = BootstrapState.COMPLETED

        } catch (exception: Exception) {
            if (state == BootstrapState.BUILDING) {
                state = BootstrapState.CREATED
            }

            throw exception
        }

        runPostHooks(contributions.toList())
    }

    @Synchronized
    fun rebuildRegistries(holder: RegistryHolder) {
        checkCompleted()

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

        executePreHooks(listOf(contribution))

        val updatedSnapshot = evaluate(contribution)

        val updatedSnapshots = snapshots.toMutableList().apply {
            set(index, updatedSnapshot)
        }

        val updatedGraph = createGraph(updatedSnapshots)

        snapshots = updatedSnapshots
        currentGraph = updatedGraph

        runPostHooks(listOf(contribution))
    }

    @Synchronized
    fun graph(): RegistryGraph {
        checkCompleted()

        return currentGraph
            ?: throw BootstrapNotCompletedException()
    }

    fun <K, V> getRegistry(
        key: RegistryKey<K, V>
    ): Registry<K, V> =
        graph().getRegistry(key)

    @Synchronized
    fun dispose() {
        if (state == BootstrapState.DISPOSED) return

        state = BootstrapState.DISPOSED

        contributions.clear()
        snapshots = emptyList()
        currentGraph = null
    }

    private fun executePreHooks(
        contributions: List<Contribution>
    ) {
        contributions.forEach { contribution ->
            contribution.preHooks.forEach { hook ->
                hook()
            }
        }
    }

    private fun runPostHooks(
        contributions: List<Contribution>
    ) {
        val exceptions = ArrayList<Exception>()

        contributions.forEach { contribution ->
            contribution.postHooks.forEach { hook ->
                try {
                    hook()
                } catch (exception: Exception) {
                    exceptions.add(exception)
                }
            }
        }

        val firstException = exceptions.firstOrNull() ?: return

        exceptions.drop(1).forEach {
            firstException.addSuppressed(it)
        }

        throw PostHookExecutionException(firstException)
    }

    @Suppress("UNCHECKED_CAST")
    private fun evaluate(
        contribution: Contribution
    ): HolderSnapshot {
        val registries = LinkedHashMap<RegistryKey<*, *>, Map<*, *>>()

        for ((key, builder) in contribution.registries) {
            val typedKey = key as RegistryKey<Any?, Any?>

            val typedBuilder = builder as RegistryContributionBuilder<Any?, Any?>.() -> Unit

            val registryBuilder = RegistryContributionBuilder(typedKey)
            registryBuilder.typedBuilder()

            registries[key] = Collections.unmodifiableMap(
                LinkedHashMap(registryBuilder.entries)
            )
        }

        return HolderSnapshot(contribution.holder, Collections.unmodifiableMap(registries))
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

            for ((holder, registries1) in snapshots) {
                val entries = registries1[key] ?: continue

                for ((entryKey, value) in entries) {
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
