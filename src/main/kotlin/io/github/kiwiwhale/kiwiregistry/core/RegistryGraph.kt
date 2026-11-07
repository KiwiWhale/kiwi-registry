package io.github.kiwiwhale.kiwiregistry.core

import io.github.kiwiwhale.kiwiregistry.exception.RegistryBuildException
import io.github.kiwiwhale.kiwiregistry.exception.RegistryNotFoundException
import io.github.kiwiwhale.kiwiregistry.exception.RegistryTypeMismatchException
import io.github.kiwiwhale.kiwiregistry.internal.DefaultRegistry
import java.util.*

/**
 * Immutable collection of registries.
 */
class RegistryGraph internal constructor(
    registries: Map<RegistryId, Registry<*, *>>
) {

    private val registries: Map<RegistryId, Registry<*, *>> =
        Collections.unmodifiableMap(
            LinkedHashMap<RegistryId, Registry<*, *>>().apply {
                for ((id, registry) in registries) {
                    require(id == registry.key.id) {
                        "Registry ID mismatch: map key '$id' " +
                                "does not match registry key '${registry.key.id}'."
                    }

                    require(id !in this) {
                        "Duplicate registry ID: '$id'."
                    }

                    put(id, snapshot(registry))
                }
            }
        )

    val size: Int
        get() = registries.size

    val isEmpty: Boolean
        get() = registries.isEmpty()

    val isNotEmpty: Boolean
        get() = registries.isNotEmpty()

    val keys: Set<RegistryId>
        get() = registries.keys

    operator fun contains(id: RegistryId): Boolean =
        id in registries

    operator fun contains(key: RegistryKey<*, *>): Boolean =
        key.id in registries

    @Suppress("UNCHECKED_CAST")
    operator fun <K : Any, V : Any> get(
        key: RegistryKey<K, V>
    ): Registry<K, V> {
        val registry = registries[key.id]
            ?: throw RegistryNotFoundException(key.id)

        if (!registry.key.hasSameTypes(key)) {
            throw RegistryTypeMismatchException(key.id)
        }

        return registry as Registry<K, V>
    }

    fun <K : Any, V : Any> find(
        key: RegistryKey<K, V>
    ): Registry<K, V>? =
        if (key.id in registries) get(key) else null

    fun all(): Collection<Registry<*, *>> =
        registries.values

    @Suppress("UNCHECKED_CAST")
    private fun snapshot(
        registry: Registry<*, *>
    ): Registry<*, *> {
        val key =
            registry.key as RegistryKey<Any, Any>

        val source = LinkedHashMap<Any, Any>()

        try {
            for ((entryKey, value) in registry.data) {
                require(key.key.isInstance(entryKey)) {
                    "Invalid key type in registry '${key.id}'."
                }

                require(key.value.isInstance(value)) {
                    "Invalid value type in registry '${key.id}'."
                }

                source[entryKey] = value
            }
        } catch (exception: Exception) {
            if (exception is RegistryBuildException) {
                throw exception
            }

            throw RegistryBuildException(
                "Failed to snapshot registry '${key.id}'.",
                exception
            )
        }

        return DefaultRegistry(key, source)
    }

    companion object {
        val EMPTY = RegistryGraph(emptyMap())
    }
}
