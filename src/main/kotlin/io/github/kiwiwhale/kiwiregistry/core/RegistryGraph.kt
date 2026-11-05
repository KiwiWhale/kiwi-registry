package io.github.kiwiwhale.kiwiregistry.core

import io.github.kiwiwhale.kiwiregistry.exception.RegistryNotFoundException
import java.util.Collections

class RegistryGraph internal constructor(
    registries: Map<RegistryKey<*, *>, Registry<*, *>>
) {

    private val registries: Map<RegistryKey<*, *>, Registry<*, *>> =
        Collections.unmodifiableMap(LinkedHashMap(registries))

    val size: Int
        get() = registries.size

    val isEmpty: Boolean
        get() = registries.isEmpty()

    val isNotEmpty: Boolean
        get() = registries.isNotEmpty()

    val keys: Set<RegistryKey<*, *>>
        get() = registries.keys

    @Suppress("UNCHECKED_CAST")
    fun <K, V> getRegistry(
        key: RegistryKey<K, V>
    ): Registry<K, V> =
        findRegistry(key)
            ?: throw RegistryNotFoundException(key.id.toString())

    @Suppress("UNCHECKED_CAST")
    fun <K, V> findRegistry(
        key: RegistryKey<K, V>
    ): Registry<K, V>? =
        registries[key] as? Registry<K, V>

    operator fun contains(
        key: RegistryKey<*, *>
    ): Boolean = key in registries

    fun all(): Collection<Registry<*, *>> =
        Collections.unmodifiableList(
            ArrayList(registries.values)
        )
}
