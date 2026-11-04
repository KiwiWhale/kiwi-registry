package io.github.kiwiwhale.kiwiregistry

import io.github.kiwiwhale.kiwiregistry.exception.DuplicateRegistryEntryException
import io.github.kiwiwhale.kiwiregistry.exception.RegistryEntryNotFoundException
import java.util.Collections
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.contains

/**
 * Thread-safe in-memory registry.
 *
 * Uses copy-on-write semantics with AtomicReference.
 *
 * Features:
 * - Lock-free reads.
 * - Atomic writes.
 * - Atomic bulk registration.
 * - Namespace-based identification.
 * - Immutable snapshots.
 *
 * @param T Registered value type.
 * @param K Registry key type.
 */
class KiwiRegistry<T : Any, K : RegistryKey<T>>(
    private val keyFactory: (RegistryId) -> K
) : Registry<T, K> {

    private data class Snapshot<K, T>(
        val byId: Map<RegistryId, T>,
        val keyById: Map<RegistryId, K>
    )

    private val state = AtomicReference(
        Snapshot<K, T>(
            byId = emptyMap(),
            keyById = emptyMap()
        )
    )

    override val size: Int
        get() = state.get().byId.size

    override fun register(key: K, value: T) {
        registerAll(linkedMapOf(key to value))
    }

    override fun register(id: RegistryId, value: T) {
        register(createKey(id), value)
    }

    override fun registerAll(additions: Map<K, T>) {
        if (additions.isEmpty()) return

        validateEntries(additions)

        while (true) {
            val current = state.get()

            val duplicates = additions.keys
                .map { it.id }
                .filter { it in current.byId }
                .distinct()
                .sortedBy { it.toString() }

            if (duplicates.isNotEmpty()) {
                throw DuplicateRegistryEntryException(
                    duplicates.joinToString()
                )
            }

            val byId = LinkedHashMap(current.byId)
            val keyById = LinkedHashMap(current.keyById)

            additions.forEach { (key, value) ->
                byId[key.id] = value
                keyById[key.id] = key
            }

            val updated = createSnapshot(byId, keyById)

            if (state.compareAndSet(current, updated)) {
                return
            }
        }
    }

    override fun registerIfAbsent(key: K, value: T): Boolean {
        while (true) {
            val current = state.get()

            if (key.id in current.byId) {
                return false
            }

            val byId = LinkedHashMap(current.byId)
            val keyById = LinkedHashMap(current.keyById)

            byId[key.id] = value
            keyById[key.id] = key

            val updated = createSnapshot(byId, keyById)

            if (state.compareAndSet(current, updated)) {
                return true
            }
        }
    }

    override fun registerIfAbsent(id: RegistryId, value: T): Boolean =
        registerIfAbsent(createKey(id), value)

    override operator fun get(key: K): T =
        get(key.id)

    override operator fun get(id: RegistryId): T =
        getOrNull(id)
            ?: throw RegistryEntryNotFoundException(id.toString())

    override fun getOrNull(key: K): T? =
        getOrNull(key.id)

    override fun getOrNull(id: RegistryId): T? =
        state.get().byId[id]

    override operator fun set(key: K, value: T) {
        if (!replace(key, value)) {
            register(key, value)
        }
    }

    override operator fun set(id: RegistryId, value: T) {
        if (!replace(id, value)) {
            register(id, value)
        }
    }

    override operator fun contains(key: K): Boolean =
        contains(key.id)

    override operator fun contains(id: RegistryId): Boolean =
        state.get().byId.containsKey(id)

    override fun unregister(key: K): Boolean =
        unregister(key.id)

    override fun unregister(id: RegistryId): Boolean {
        while (true) {
            val current = state.get()

            if (id !in current.byId) {
                return false
            }

            val byId = LinkedHashMap(current.byId)
            val keyById = LinkedHashMap(current.keyById)

            byId.remove(id)
            keyById.remove(id)

            val updated = createSnapshot(byId, keyById)

            if (state.compareAndSet(current, updated)) {
                return true
            }
        }
    }

    override fun replace(key: K, value: T): Boolean =
        replace(key.id, value)

    override fun replace(id: RegistryId, value: T): Boolean {
        while (true) {
            val current = state.get()

            if (id !in current.byId) {
                return false
            }

            val byId = LinkedHashMap(current.byId)
            byId[id] = value

            val updated = createSnapshot(
                byId,
                current.keyById
            )

            if (state.compareAndSet(current, updated)) {
                return true
            }
        }
    }

    override fun clear() {
        while (true) {
            val current = state.get()

            if (current.byId.isEmpty()) {
                return
            }

            val empty = Snapshot<K, T>(
                byId = emptyMap(),
                keyById = emptyMap()
            )

            if (state.compareAndSet(current, empty)) {
                return
            }
        }
    }

    override fun isEmpty(): Boolean =
        state.get().byId.isEmpty()

    override fun keys(): Set<K> =
        immutableSet(state.get().keyById.values)

    override fun ids(): Set<RegistryId> =
        state.get().byId.keys

    override fun snapshot(): Map<K, T> {
        val current = state.get()

        val result = LinkedHashMap<K, T>()

        current.keyById.forEach { (id, key) ->
            result[key] = current.byId.getValue(id)
        }

        return immutableMap(result)
    }

    private fun createKey(id: RegistryId): K {
        val key = keyFactory(id)

        require(key.id == id) {
            "Generated key ID '${key.id}' does not match '$id'."
        }

        return key
    }

    private fun validateEntries(additions: Map<K, T>) {
        val duplicates = additions.keys
            .groupBy { it.id }
            .filterValues { it.size > 1 }
            .keys

        if (duplicates.isNotEmpty()) {
            throw DuplicateRegistryEntryException(
                duplicates
                    .sortedBy { it.toString() }
                    .joinToString()
            )
        }
    }

    private fun createSnapshot(
        byId: Map<RegistryId, T>,
        keyById: Map<RegistryId, K>
    ): Snapshot<K, T> =
        Snapshot(
            byId = immutableMap(byId),
            keyById = immutableMap(keyById)
        )

    private fun <A, B> immutableMap(map: Map<A, B>): Map<A, B> =
        Collections.unmodifiableMap(LinkedHashMap(map))

    private fun <V> immutableSet(values: Collection<V>): Set<V> =
        Collections.unmodifiableSet(LinkedHashSet(values))
}
