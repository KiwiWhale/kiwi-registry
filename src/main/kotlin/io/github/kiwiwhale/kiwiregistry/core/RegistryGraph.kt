package io.github.kiwiwhale.kiwiregistry.core

import io.github.kiwiwhale.kiwiregistry.exception.RegistryBuildException
import io.github.kiwiwhale.kiwiregistry.exception.RegistryNotFoundException
import io.github.kiwiwhale.kiwiregistry.exception.RegistryTypeMismatchException
import io.github.kiwiwhale.kiwiregistry.internal.DefaultRegistry
import java.util.Collections

/**
 * Immutable collection of registries.
 *
 * Each registry is copied into an independent snapshot during
 * construction.
 *
 * The graph structure and registry maps cannot be modified
 * after construction.
 *
 * Individual key and value objects are not deep-copied.
 *
 * @param registries Registries used to construct this graph.
 */
class RegistryGraph internal constructor(
    registries: Map<RegistryId, Registry<*, *>>
) {

    private val registries: Map<RegistryId, Registry<*, *>> =
        Collections.unmodifiableMap(
            createSnapshots(registries)
        )

    private fun createSnapshots(
        source: Map<RegistryId, Registry<*, *>>
    ): LinkedHashMap<RegistryId, Registry<*, *>> {
        val snapshots = LinkedHashMap<RegistryId, Registry<*, *>>()

        for ((id, registry) in source) {
            require(id == registry.key.id) {
                "Registry ID mismatch: map key '$id' " +
                        "does not match registry key '${registry.key.id}'."
            }

            snapshots[id] = snapshot(registry)
        }

        return snapshots
    }

    /**
     * Number of registries contained in this graph.
     */
    val size: Int
        get() = registries.size

    /**
     * Whether this graph contains no registries.
     */
    val isEmpty: Boolean
        get() = registries.isEmpty()

    /**
     * Whether this graph contains at least one registry.
     */
    val isNotEmpty: Boolean
        get() = registries.isNotEmpty()

    /**
     * IDs of all registries contained in this graph.
     */
    val keys: Set<RegistryId>
        get() = registries.keys

    /**
     * Checks whether a registry with the specified [id] exists.
     *
     * @param id Registry identifier.
     * @return True if the registry exists.
     */
    operator fun contains(id: RegistryId): Boolean =
        id in registries

    /**
     * Checks whether a registry with the specified [key]'s ID exists.
     *
     * This operation checks only the registry ID.
     * It does not verify runtime type compatibility.
     *
     * @param key Typed registry identifier.
     * @return True if a registry with the same ID exists.
     */
    operator fun contains(key: RegistryKey<*, *>): Boolean =
        key.id in registries

    /**
     * Returns the registry associated with [key].
     *
     * Both the registry ID and its runtime key and value types
     * must match.
     *
     * @param key Typed registry identifier.
     * @return Matching registry.
     *
     * @throws RegistryNotFoundException If no registry with the
     * specified ID exists.
     * @throws RegistryTypeMismatchException If the registry exists
     * but has incompatible runtime types.
     */
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

    /**
     * Finds a registry associated with [key].
     *
     * Returns null only when the registry does not exist.
     * Type mismatches are reported as exceptions.
     *
     * @param key Typed registry identifier.
     * @return Matching registry, or null if absent.
     *
     * @throws RegistryTypeMismatchException If the registry exists
     * but has incompatible runtime types.
     */
    fun <K : Any, V : Any> find(
        key: RegistryKey<K, V>
    ): Registry<K, V>? =
        if (key.id in registries) get(key) else null

    /**
     * Returns all registries in insertion order.
     *
     * The returned collection cannot be modified.
     *
     * @return Immutable collection of registries.
     */
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

        /**
         * Shared empty registry graph.
         */
        val EMPTY = RegistryGraph(emptyMap())
    }
}
