package io.github.kiwiwhale.kiwiregistry

import io.github.kiwiwhale.kiwiregistry.exception.DuplicateRegistryEntryException
import io.github.kiwiwhale.kiwiregistry.exception.RegistryEntryNotFoundException
import java.util.Collections
import java.util.concurrent.atomic.AtomicReference

/**
 * Thread-safe in-memory registry.
 *
 * Uses copy-on-write semantics with AtomicReference.
 *
 * Features:
 * - Lock-free reads.
 * - Atomic writes.
 * - Atomic bulk registration.
 * - Namespace isolation.
 * - Immutable snapshots.
 *
 * @param T Base registered value type.
 * @property namespace Registry namespace.
 */
class KiwiRegistry<T : Any>(
    val namespace: String
) : Registry<T> {

    private val state = AtomicReference<Map<RegistryId, T>>(emptyMap())

    init {
        require(namespace.isNotBlank() && ':' !in namespace) {
            "Invalid registry namespace: '$namespace'."
        }
    }

    override val size: Int
        get() = state.get().size

    override fun <V : T> register(key: RegistryKey<V>, value: V) {
        register(key.id, value)
    }

    override fun register(id: RegistryId, value: T) {
        validateId(id)

        registerAll(mapOf(id to value))
    }

    override fun registerAll(additions: Map<RegistryId, T>) {
        if (additions.isEmpty()) return

        additions.keys.forEach(::validateId)

        while (true) {
            val current = state.get()

            val duplicates = additions.keys
                .filter { it in current }
                .sortedBy(RegistryId::toString)

            if (duplicates.isNotEmpty()) {
                throw DuplicateRegistryEntryException(
                    duplicates.joinToString()
                )
            }

            val updated = LinkedHashMap(current)
            updated.putAll(additions)

            if (state.compareAndSet(current, immutableMap(updated))) {
                return
            }
        }
    }

    override fun <V : T> registerIfAbsent(
        key: RegistryKey<V>,
        value: V
    ): Boolean = registerIfAbsent(key.id, value)

    override fun registerIfAbsent(
        id: RegistryId,
        value: T
    ): Boolean {
        validateId(id)

        while (true) {
            val current = state.get()

            if (id in current) return false

            val updated = LinkedHashMap(current)
            updated[id] = value

            if (state.compareAndSet(current, immutableMap(updated))) {
                return true
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <V : T> get(key: RegistryKey<V>): V =
        get(key.id) as V

    override fun get(id: RegistryId): T =
        getOrNull(id)
            ?: throw RegistryEntryNotFoundException(id.toString())

    @Suppress("UNCHECKED_CAST")
    override fun <V : T> getOrNull(key: RegistryKey<V>): V? =
        getOrNull(key.id) as? V

    override fun getOrNull(id: RegistryId): T? {
        validateId(id)
        return state.get()[id]
    }

    override fun <V : T> set(key: RegistryKey<V>, value: V) {
        set(key.id, value)
    }

    override fun set(id: RegistryId, value: T) {
        validateId(id)

        while (true) {
            val current = state.get()
            val updated = LinkedHashMap(current)

            updated[id] = value

            if (state.compareAndSet(current, immutableMap(updated))) {
                return
            }
        }
    }

    override fun contains(key: RegistryKey<*>): Boolean =
        contains(key.id)

    override fun contains(id: RegistryId): Boolean {
        validateId(id)
        return state.get().containsKey(id)
    }

    override fun unregister(key: RegistryKey<*>): Boolean =
        unregister(key.id)

    override fun unregister(id: RegistryId): Boolean {
        validateId(id)

        while (true) {
            val current = state.get()

            if (id !in current) return false

            val updated = LinkedHashMap(current)
            updated.remove(id)

            if (state.compareAndSet(current, immutableMap(updated))) {
                return true
            }
        }
    }

    override fun replace(id: RegistryId, value: T): Boolean {
        validateId(id)

        while (true) {
            val current = state.get()

            if (id !in current) return false

            val updated = LinkedHashMap(current)
            updated[id] = value

            if (state.compareAndSet(current, immutableMap(updated))) {
                return true
            }
        }
    }

    override fun clear() {
        state.set(emptyMap())
    }

    override fun isEmpty(): Boolean =
        state.get().isEmpty()

    override fun ids(): Set<RegistryId> =
        state.get().keys

    override fun snapshot(): Map<RegistryId, T> =
        state.get()

    private fun validateId(id: RegistryId) {
        require(id.namespace == namespace) {
            "Expected namespace '$namespace', got '${id.namespace}'."
        }
    }

    private fun immutableMap(
        map: Map<RegistryId, T>
    ): Map<RegistryId, T> =
        Collections.unmodifiableMap(LinkedHashMap(map))
}
