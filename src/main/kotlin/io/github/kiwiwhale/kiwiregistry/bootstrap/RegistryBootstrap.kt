package io.github.kiwiwhale.kiwiregistry.bootstrap

import io.github.kiwiwhale.kiwiregistry.core.Registry
import io.github.kiwiwhale.kiwiregistry.core.RegistryGraph
import io.github.kiwiwhale.kiwiregistry.core.RegistryKey
import io.github.kiwiwhale.kiwiregistry.exception.BootstrapAlreadyCompletedException
import io.github.kiwiwhale.kiwiregistry.exception.DuplicateContributionException

internal class RegistryBootstrap {

    private val contributions = ArrayList<Contribution>()
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

        val graph = buildGraph(contributions)

        currentGraph = graph

        contributions.forEach { contribution ->
            contribution.postHooks.forEach { it() }
        }
    }

    @Synchronized
    fun rebuildRegistries(holder: RegistryHolder) {
        val current = currentGraph
            ?: error("Registry bootstrap has not been completed")

        val contribution = contributions.firstOrNull {
            it.holder === holder
        } ?: throw IllegalArgumentException("No contribution found for holder: ${holder::class.qualifiedName}")

        contribution.preHooks.forEach { it() }

        val rebuiltGraph = buildGraph(contributions)
        currentGraph = rebuiltGraph

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

    private fun buildGraph(
        contributions: List<Contribution>
    ): RegistryGraph {
        val definitions = LinkedHashMap<RegistryKey<*, *>, RegistryDefinition<*, *>>()

        contributions.forEach { contribution ->
            contribution.registries.forEach { (key, builder) ->
                addDefinition(
                    definitions,
                    key,
                    contribution.holder,
                    builder
                )
            }
        }

        val registries = definitions.values
            .map { it.build() }
            .associateBy { it.registryKey }

        return RegistryGraph(registries)
    }

    @Suppress("UNCHECKED_CAST")
    private fun addDefinition(
        definitions: MutableMap<RegistryKey<*, *>, RegistryDefinition<*, *>>,
        key: RegistryKey<*, *>,
        holder: RegistryHolder,
        builder: RegistryContributionBuilder<*, *>.() -> Unit
    ) {
        val typedKey = key as RegistryKey<Any?, Any?>

        val definition = definitions.getOrPut(key) {
            RegistryDefinition(typedKey)
        } as RegistryDefinition<Any?, Any?>

        val typedBuilder = builder as RegistryContributionBuilder<Any?, Any?>.() -> Unit

        definition.add(holder, typedBuilder)
    }
}
