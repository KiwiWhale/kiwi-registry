package io.github.kiwiwhale.kiwiregistry.core

class RegistryGraph internal constructor(
    registries: Map<RegistryKey<*, *>, Registry<*, *>>
) {

    private val registries =
        registries.toMap()

    @Suppress("UNCHECKED_CAST")
    fun <K, V> getRegistry(
        key: RegistryKey<K, V>
    ): Registry<K, V> {
        return registries[key] as? Registry<K, V>
            ?: error("Registry not found: ${key.id}")
    }

    internal fun rebuild(
        registry: Registry<*, *>
    ): RegistryGraph {
        val updated = registries.toMutableMap()

        updated[registry.registryKey] = registry

        return RegistryGraph(updated)
    }

    internal fun rebuild(
        registries: Collection<Registry<*, *>>
    ): RegistryGraph {
        val updated = this.registries.toMutableMap()

        registries.forEach { registry ->
            updated[registry.registryKey] = registry
        }

        return RegistryGraph(updated)
    }

    fun contains(key: RegistryKey<*, *>): Boolean =
        key in registries

    val size: Int
        get() = registries.size

    fun all(): Collection<Registry<*, *>> =
        registries.values.toList()
}
