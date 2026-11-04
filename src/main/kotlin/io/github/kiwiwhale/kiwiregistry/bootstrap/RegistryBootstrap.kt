package io.github.kiwiwhale.kiwiregistry.bootstrap

import io.github.kiwiwhale.kiwiregistry.core.Registry
import io.github.kiwiwhale.kiwiregistry.core.RegistryGraph
import io.github.kiwiwhale.kiwiregistry.core.RegistryKey
import io.github.kiwiwhale.kiwiregistry.exception.BootstrapAlreadyCompletedException
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

    val isInitialized: Boolean
        get() = currentGraph != null

    @Synchronized
    fun submit(contribution: Contribution) {
        if (isInitialized) {
            throw BootstrapAlreadyCompletedException()
        }

        if (contributions.any { it.holder === contribution.holder }) {
            throw DuplicateContributionException(contribution.holder)
        }

        contributions += contribution
    }

    @Synchronized
    fun build() {
        if (isInitialized) {
            throw BootstrapAlreadyCompletedException()
        }

        contributions.forEach { contribution ->
            contribution.preHooks.forEach { it() }
        }

        val newSnapshots = contributions.map { contribution ->
            evaluate(contribution)
        }

        val newGraph = createGraph(newSnapshots)

        snapshots = newSnapshots
        currentGraph = newGraph

        contributions.forEach { contribution ->
            contribution.postHooks.forEach { it() }
        }
    }

    @Synchronized
    fun rebuildRegistries(holder: RegistryHolder) {
        if (!isInitialized) {
            throw InvalidContributionException("Registry bootstrap has not been completed")
        }

        val index = snapshots.indexOfFirst {
            it.holder === holder
        }

        if (index == -1) {
            throw InvalidContributionException(
                "No contribution found for holder: ${holder::class.qualifiedName}"
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

        contribution.postHooks.forEach { it() }
    }

    fun graph(): RegistryGraph {
        return currentGraph
            ?: error("Registry bootstrap has not been completed")
    }

    fun <K, V> getRegistry(
        key: RegistryKey<K, V>
    ): Registry<K, V> {
        return graph().getRegistry(key)
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

            val registryBuilder =
                RegistryContributionBuilder(typedKey)

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

            for ((holder, registries1) in snapshots) {

                val entries = registries1[key] as? Map<Any?, Any?>
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
