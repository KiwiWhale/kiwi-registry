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
 * The graph structure and registry entry maps cannot be modified
 * after construction.
 *
 * Individual key and value objects are not deep-copied.
 *
 * Registry insertion order is preserved.
 *
 * @param registries Registries used to construct this graph.
 */
class RegistryGraph internal constructor(
    registries: Map<RegistryId, Registry<*, *>>
) {

    private val registries: Map<RegistryId, Registry<*, *>> =
        Collections.unmodifiableMap(
            snapshotRegistries(registries)
        )

    /**
     * Creates immutable snapshots of the supplied registries.
     *
     * @param source Source registries.
     * @return Ordered map containing immutable registry snapshots.
     * @throws IllegalArgumentException If a registry ID does not match its key.
     * @throws RegistryBuildException If a registry cannot be copied.
     */
    private fun snapshotRegistries(
        source: Map<RegistryId, Registry<*, *>>
    ): LinkedHashMap<RegistryId, Registry<*, *>> =
        LinkedHashMap<RegistryId, Registry<*, *>>().apply {
            for ((id, registry) in source) {
                require(id == registry.key.id) {
                    "Registry ID mismatch: map key '$id' " +
                            "does not match registry key '${registry.key.id}'."
                }

                put(id, snapshot(registry))
            }
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
     * Registry IDs in insertion order.
     */
    val registryIds: Set<RegistryId>
        get() = registries.keys

    /**
     * Alias for [registryIds].
     */
    val keys: Set<RegistryId>
        get() = registryIds

    /**
     * Checks whether a registry with the specified ID exists.
     *
     * @param id Registry identifier.
     * @return True if the registry exists.
     */
    operator fun contains(id: RegistryId): Boolean =
        id in registries

    /**
     * Checks whether a registry with the specified key's ID exists.
     *
     * This operation checks only the registry ID.
     * Runtime type compatibility is not verified.
     *
     * @param key Typed registry identifier.
     * @return True if a registry with the same ID exists.
     */
    operator fun contains(key: RegistryKey<*, *>): Boolean =
        key.id in registries

    /**
     * Returns the registry associated with the specified key.
     *
     * @param key Typed registry identifier.
     * @return Associated registry.
     * @throws RegistryNotFoundException If the registry does not exist.
     * @throws RegistryTypeMismatchException If its types do not match.
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
     * Returns a registry by ID.
     *
     * @param id Registry identifier.
     * @return Associated registry, or null if absent.
     */
    fun getOrNull(id: RegistryId): Registry<*, *>? =
        registries[id]

    /**
     * Returns a registry by its typed key.
     *
     * @param key Typed registry identifier.
     * @return Associated registry, or null if absent.
     * @throws RegistryTypeMismatchException If its types do not match.
     */
    fun <K : Any, V : Any> getOrNull(
        key: RegistryKey<K, V>
    ): Registry<K, V>? =
        registries[key.id]?.let {
            get(key)
        }

    /**
     * Finds a registry using its typed key.
     *
     * This is an alias for [getOrNull].
     *
     * @param key Typed registry identifier.
     * @return Associated registry, or null if absent.
     * @throws RegistryTypeMismatchException If its types do not match.
     */
    fun <K : Any, V : Any> find(
        key: RegistryKey<K, V>
    ): Registry<K, V>? =
        getOrNull(key)

    /**
     * Performs the specified action for each registry.
     *
     * Registries are visited in insertion order.
     *
     * @param action Operation performed for each registry.
     */
    fun forEach(
        action: (RegistryId, Registry<*, *>) -> Unit
    ) {
        registries.forEach(action)
    }

    /**
     * Returns all registries in insertion order.
     *
     * @return Read-only collection of registries.
     */
    fun all(): Collection<Registry<*, *>> =
        registries.values

    /**
     * Creates an immutable snapshot of the specified registry.
     *
     * Entry keys and values are validated against the registry's
     * declared runtime types.
     *
     * @param registry Source registry.
     * @return Immutable registry snapshot.
     * @throws RegistryBuildException If snapshot creation fails.
     */
    @Suppress("UNCHECKED_CAST")
    private fun snapshot(
        registry: Registry<*, *>
    ): Registry<*, *> {
        val key = registry.key as RegistryKey<Any, Any>

        try {
            val entries = LinkedHashMap<Any, Any>()

            for ((entryKey, value) in registry.data) {
                require(key.key.isInstance(entryKey)) {
                    "Invalid key type in registry '${key.id}'."
                }

                require(key.value.isInstance(value)) {
                    "Invalid value type in registry '${key.id}'."
                }

                entries[entryKey] = value
            }

            return DefaultRegistry(key, entries)
        } catch (exception: Exception) {
            if (exception is RegistryBuildException) {
                throw exception
            }

            throw RegistryBuildException(
                "Failed to snapshot registry '${key.id}'.",
                exception
            )
        }
    }

    companion object {

        /**
         * Shared empty registry graph.
         */
        val EMPTY = RegistryGraph(emptyMap())

        private class MergedRegistry(
            val key: RegistryKey<*, *>,
            val entries: LinkedHashMap<Any, Any>
        )

        /**
         * Merges multiple registry graphs into a new graph.
         *
         * Registries with the same ID must have compatible runtime types.
         *
         * When duplicate entry keys are encountered, values from
         * later graphs replace values from earlier graphs.
         *
         * Registry and entry insertion order is preserved.
         * Existing graphs are never modified.
         *
         * @param graphs Registry graphs to merge, in merge order.
         * @return A new merged registry graph, or [EMPTY] if no graphs are supplied.
         * @throws RegistryTypeMismatchException If registries with
         * the same ID have incompatible types.
         * @throws RegistryBuildException If the merged graph cannot
         * be constructed.
         */
        fun merge(
            vararg graphs: RegistryGraph
        ): RegistryGraph {
            if (graphs.isEmpty()) {
                return EMPTY
            }

            val merged = LinkedHashMap<RegistryId, MergedRegistry>()

            for (graph in graphs) {
                for ((id, registry) in graph.registries) {
                    val existing = merged[id]

                    if (existing == null) {
                        @Suppress("UNCHECKED_CAST")
                        val entries = LinkedHashMap(
                            registry.data as Map<Any, Any>
                        )

                        merged[id] = MergedRegistry(
                            key = registry.key,
                            entries = entries
                        )

                        continue
                    }

                    if (!existing.key.hasSameTypes(registry.key)) {
                        throw RegistryTypeMismatchException(id)
                    }

                    @Suppress("UNCHECKED_CAST")
                    existing.entries.putAll(
                        registry.data as Map<Any, Any>
                    )
                }
            }

            val result = LinkedHashMap<RegistryId, Registry<*, *>>()

            for ((id, mergedRegistry) in merged) {
                @Suppress("UNCHECKED_CAST")
                val key = mergedRegistry.key as RegistryKey<Any, Any>

                result[id] = DefaultRegistry(
                    key = key,
                    source = mergedRegistry.entries
                )
            }

            return RegistryGraph(result)
        }
    }
}
