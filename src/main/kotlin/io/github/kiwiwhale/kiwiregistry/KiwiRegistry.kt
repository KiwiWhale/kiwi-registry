package io.github.kiwiwhale.kiwiregistry

import io.github.kiwiwhale.kiwiregistry.exception.DuplicateRegistryEntryException
import io.github.kiwiwhale.kiwiregistry.exception.RegistryEntryNotFoundException
import java.util.Collections
import java.util.concurrent.atomic.AtomicReference

/**
 * Thread-safe, namespace-scoped in-memory registry.
 *
 * Uses immutable copy-on-write state with AtomicReference.
 *
 * Features:
 * - Lock-free reads.
 * - Atomic writes.
 * - Atomic bulk registration.
 * - Namespace isolation.
 * - Typed and string-based keys.
 * - Immutable snapshots.
 *
 * @param T Registered value type.
 * @property namespace Registry namespace.
 */
class KiwiRegistry<T : Any>(
    override val namespace: String
) : Registry<T> {

    /**
     * Immutable registry state.
     *
     * Invariant:
     * byId.keys == keyById.keys
     */
    private data class State<T : Any>(
        val byId: Map<RegistryId, T>,
        val keyById: Map<RegistryId, RegistryKey<T>>
    )

    private val state = AtomicReference(emptyState<T>())

    init {
        require(namespace.isNotBlank() && ':' !in namespace) {
            "Invalid registry namespace: '$namespace'."
        }
    }

    override val size: Int
        get() = state.get().byId.size

    override fun register(name: String, value: T) {
        register(id(name), value)
    }

    override fun register(id: RegistryId, value: T) {
        register(defaultKey(id), value)
    }

    override fun register(
        key: RegistryKey<T>,
        value: T
    ) {
        registerAll(mapOf(key to value))
    }

    override fun registerAll(
        additions: Map<out RegistryKey<T>, T>
    ) {
        if (additions.isEmpty()) return

        val entries = LinkedHashMap<
                RegistryId,
                Pair<RegistryKey<T>, T>
                >()

        additions.forEach { (key, value) ->
            validateId(key.id)

            require(entries.put(key.id, key to value) == null) {
                "Duplicate registry ID: '${key.id}'."
            }
        }

        while (true) {
            val current = state.get()

            val duplicates = entries.keys
                .filter { it in current.byId }
                .sortedBy(RegistryId::toString)

            if (duplicates.isNotEmpty()) {
                throw DuplicateRegistryEntryException(
                    duplicates.map(RegistryId::toString)
                )
            }

            validateKeyUniqueness(
                current.keyById,
                entries.mapValues { it.value.first }
            )

            val byId = LinkedHashMap(current.byId)
            val keyById = LinkedHashMap(current.keyById)

            entries.forEach { (id, entry) ->
                val (key, value) = entry

                byId[id] = value
                keyById[id] = key
            }

            val updated = createState(byId, keyById)

            if (state.compareAndSet(current, updated)) {
                return
            }
        }
    }

    override fun registerAllByName(
        additions: Map<String, T>
    ) {
        if (additions.isEmpty()) return

        val keyedAdditions = LinkedHashMap<RegistryKey<T>, T>()

        additions.forEach { (name, value) ->
            val key = defaultKey(id(name))
            keyedAdditions[key] = value
        }

        registerAll(keyedAdditions)
    }

    override fun registerIfAbsent(
        name: String,
        value: T
    ): Boolean =
        registerIfAbsent(id(name), value)

    override fun registerIfAbsent(
        id: RegistryId,
        value: T
    ): Boolean =
        registerIfAbsent(defaultKey(id), value)

    override fun registerIfAbsent(
        key: RegistryKey<T>,
        value: T
    ): Boolean {
        validateId(key.id)

        while (true) {
            val current = state.get()

            if (key.id in current.byId) {
                return false
            }

            validateKeyUniqueness(
                current.keyById,
                mapOf(key.id to key)
            )

            val byId = LinkedHashMap(current.byId)
            val keyById = LinkedHashMap(current.keyById)

            byId[key.id] = value
            keyById[key.id] = key

            val updated = createState(byId, keyById)

            if (state.compareAndSet(current, updated)) {
                return true
            }
        }
    }

    override operator fun get(name: String): T =
        get(id(name))

    override operator fun get(id: RegistryId): T {
        validateId(id)

        return getOrNull(id)
            ?: throw RegistryEntryNotFoundException(id.toString())
    }

    override operator fun get(key: RegistryKey<T>): T =
        get(key.id)

    override fun getOrNull(name: String): T? =
        getOrNull(id(name))

    override fun getOrNull(id: RegistryId): T? {
        validateId(id)
        return state.get().byId[id]
    }

    override fun getOrNull(key: RegistryKey<T>): T? =
        getOrNull(key.id)

    override operator fun set(name: String, value: T) {
        set(id(name), value)
    }

    override operator fun set(id: RegistryId, value: T) {
        validateId(id)

        upsert(
            id = id,
            key = defaultKey(id),
            value = value,
            replaceKey = false
        )
    }

    override operator fun set(
        key: RegistryKey<T>,
        value: T
    ) {
        validateId(key.id)

        upsert(
            id = key.id,
            key = key,
            value = value,
            replaceKey = true
        )
    }

    private fun upsert(
        id: RegistryId,
        key: RegistryKey<T>,
        value: T,
        replaceKey: Boolean
    ) {
        while (true) {
            val current = state.get()

            if (replaceKey) {
                validateKeyUniqueness(
                    current.keyById,
                    mapOf(id to key)
                )
            }

            val byId = LinkedHashMap(current.byId)
            val keyById = LinkedHashMap(current.keyById)

            byId[id] = value

            if (replaceKey || id !in keyById) {
                keyById[id] = key
            }

            val updated = createState(byId, keyById)

            if (state.compareAndSet(current, updated)) {
                return
            }
        }
    }

    override fun replace(
        name: String,
        value: T
    ): Boolean =
        replace(id(name), value)

    override fun replace(
        id: RegistryId,
        value: T
    ): Boolean {
        validateId(id)

        while (true) {
            val current = state.get()

            if (id !in current.byId) {
                return false
            }

            val byId = LinkedHashMap(current.byId)
            byId[id] = value

            val updated = createState(
                byId = byId,
                keyById = current.keyById
            )

            if (state.compareAndSet(current, updated)) {
                return true
            }
        }
    }

    override fun replace(
        key: RegistryKey<T>,
        value: T
    ): Boolean =
        replace(key.id, value)

    override fun unregister(name: String): Boolean =
        unregister(id(name))

    override fun unregister(id: RegistryId): Boolean {
        validateId(id)

        while (true) {
            val current = state.get()

            if (id !in current.byId) {
                return false
            }

            val byId = LinkedHashMap(current.byId)
            val keyById = LinkedHashMap(current.keyById)

            byId.remove(id)
            keyById.remove(id)

            val updated = createState(byId, keyById)

            if (state.compareAndSet(current, updated)) {
                return true
            }
        }
    }

    override fun unregister(
        key: RegistryKey<T>
    ): Boolean =
        unregister(key.id)

    override fun clear() {
        while (true) {
            val current = state.get()

            if (current.byId.isEmpty()) {
                return
            }

            if (state.compareAndSet(current, emptyState())) {
                return
            }
        }
    }

    override fun isEmpty(): Boolean =
        state.get().byId.isEmpty()

    override operator fun contains(name: String): Boolean =
        contains(id(name))

    override operator fun contains(id: RegistryId): Boolean {
        validateId(id)
        return state.get().byId.containsKey(id)
    }

    override operator fun contains(
        key: RegistryKey<T>
    ): Boolean =
        contains(key.id)

    override fun keys(): Set<RegistryKey<T>> =
        immutableSet(state.get().keyById.values)

    override fun ids(): Set<RegistryId> =
        immutableSet(state.get().byId.keys)

    override fun names(): Set<String> =
        immutableSet(
            state.get().byId.keys.map { it.key }
        )

    override fun snapshot(): Map<RegistryKey<T>, T> {
        val current = state.get()
        val result = LinkedHashMap<RegistryKey<T>, T>()

        current.keyById.forEach { (id, key) ->
            result[key] = current.byId.getValue(id)
        }

        check(result.size == current.byId.size) {
            "RegistryKey equality collision detected."
        }

        return immutableMap(result)
    }

    override fun snapshotById(): Map<RegistryId, T> =
        state.get().byId

    private fun id(name: String): RegistryId =
        RegistryId(namespace, name)

    private fun defaultKey(
        id: RegistryId
    ): KiwiRegistryKey<T> {
        validateId(id)
        return KiwiRegistryKey(id)
    }

    private fun validateId(id: RegistryId) {
        require(id.namespace == namespace) {
            "Invalid registry namespace: '${id.namespace}'. " +
                    "Expected '$namespace'."
        }
    }

    /**
     * Prevents different IDs from being represented by equal keys.
     *
     * This is required because snapshot() uses RegistryKey as Map keys.
     */
    private fun validateKeyUniqueness(
        existing: Map<RegistryId, RegistryKey<T>>,
        additions: Map<RegistryId, RegistryKey<T>>
    ) {
        val keys = HashMap<RegistryKey<T>, RegistryId>()

        existing.forEach { (id, key) ->
            if (id !in additions) {
                val previous = keys.put(key, id)

                require(previous == null) {
                    "Different registry IDs must not have equal keys."
                }
            }
        }

        additions.forEach { (id, key) ->
            val previous = keys.put(key, id)

            require(previous == null) {
                "Different registry IDs must not have equal keys: " +
                        "'$previous' and '$id'."
            }
        }
    }

    private fun createState(
        byId: Map<RegistryId, T>,
        keyById: Map<RegistryId, RegistryKey<T>>
    ): State<T> {
        check(byId.keys == keyById.keys) {
            "Registry state is inconsistent."
        }

        return State(
            byId = immutableMap(byId),
            keyById = immutableMap(keyById)
        )
    }

    private fun <A, B> immutableMap(
        map: Map<A, B>
    ): Map<A, B> =
        Collections.unmodifiableMap(
            LinkedHashMap(map)
        )

    private fun <V> immutableSet(
        values: Collection<V>
    ): Set<V> =
        Collections.unmodifiableSet(
            LinkedHashSet(values)
        )

    private fun <T : Any> emptyState(): State<T> =
        State(
            byId = emptyMap(),
            keyById = emptyMap()
        )
}
