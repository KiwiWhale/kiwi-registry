package io.github.kiwiwhale.kiwiregistry.bootstrap

import io.github.kiwiwhale.kiwiregistry.core.RegistryKey
import io.github.kiwiwhale.kiwiregistry.registry.FrozenRegistry
import io.github.kiwiwhale.kiwiregistry.registry.MutableRegistry

internal class RegistryDefinition<K, V>(
    val registryKey: RegistryKey<K, V>
) {

    private val builders = LinkedHashMap<RegistryHolder, RegistryContributionBuilder<K, V>.() -> Unit>()

    fun add(
        holder: RegistryHolder,
        builder: RegistryContributionBuilder<K, V>.() -> Unit
    ) {
        builders[holder] = builder
    }

    fun build(): FrozenRegistry<K, V> {
        val registry = MutableRegistry(registryKey)

        builders.forEach { (holder, builder) ->
            val contribution = RegistryContributionBuilder(registryKey)

            contribution.builder()

            contribution.entries.forEach { (key, value) ->
                registry.register(holder, key, value)
            }
        }

        return registry.freeze()
    }
}
