package io.github.kiwiwhale.kiwiregistry

/**
 * Contract for a namespace-scoped registry.
 *
 * @param T Registered value type.
 */
interface Registry<T : Any> {

    val namespace: String

    val size: Int

    fun register(name: String, value: T)

    fun register(id: RegistryId, value: T)

    fun register(key: RegistryKey<T>, value: T)

    /**
     * Atomically registers multiple entries.
     *
     * If any entry conflicts with an existing ID,
     * no entries from the batch are registered.
     */
    fun registerAll(
        additions: Map<out RegistryKey<T>, T>
    )

    /**
     * Registers multiple entries using string keys.
     */
    fun registerAllByName(
        additions: Map<String, T>
    )

    fun registerIfAbsent(name: String, value: T): Boolean

    fun registerIfAbsent(id: RegistryId, value: T): Boolean

    fun registerIfAbsent(
        key: RegistryKey<T>,
        value: T
    ): Boolean

    operator fun get(name: String): T

    operator fun get(id: RegistryId): T

    operator fun get(key: RegistryKey<T>): T

    fun getOrNull(name: String): T?

    fun getOrNull(id: RegistryId): T?

    fun getOrNull(key: RegistryKey<T>): T?

    operator fun set(name: String, value: T)

    operator fun set(id: RegistryId, value: T)

    operator fun set(key: RegistryKey<T>, value: T)

    operator fun contains(name: String): Boolean

    operator fun contains(id: RegistryId): Boolean

    operator fun contains(key: RegistryKey<T>): Boolean

    fun replace(name: String, value: T): Boolean

    fun replace(id: RegistryId, value: T): Boolean

    fun replace(key: RegistryKey<T>, value: T): Boolean

    fun unregister(name: String): Boolean

    fun unregister(id: RegistryId): Boolean

    fun unregister(key: RegistryKey<T>): Boolean

    fun clear()

    fun isEmpty(): Boolean

    fun keys(): Set<RegistryKey<T>>

    fun ids(): Set<RegistryId>

    fun names(): Set<String>

    /**
     * Returns an immutable snapshot indexed by typed keys.
     */
    fun snapshot(): Map<RegistryKey<T>, T>

    /**
     * Returns an immutable snapshot indexed by registry IDs.
     */
    fun snapshotById(): Map<RegistryId, T>
}
