package io.github.kiwiwhale.kiwiregistry

/**
 * Contract for managing registry entries.
 *
 * @param T Base registered value type.
 */
interface Registry<T : Any> {

    /** Number of registered entries. */
    val size: Int

    /** Registers an entry using a typed key. */
    fun <V : T> register(key: RegistryKey<V>, value: V)

    /** Registers an entry using its identifier. */
    fun register(id: RegistryId, value: T)

    /** Atomically registers multiple entries. */
    fun registerAll(additions: Map<RegistryId, T>)

    /** Registers an entry only if absent. */
    fun <V : T> registerIfAbsent(
        key: RegistryKey<V>,
        value: V
    ): Boolean

    /** Registers an entry only if its ID is absent. */
    fun registerIfAbsent(id: RegistryId, value: T): Boolean

    /** Retrieves an entry using a typed key. */
    operator fun <V : T> get(key: RegistryKey<V>): V

    /** Retrieves an entry using its identifier. */
    operator fun get(id: RegistryId): T

    /** Retrieves an entry or returns null. */
    fun <V : T> getOrNull(key: RegistryKey<V>): V?

    /** Retrieves an entry or returns null. */
    fun getOrNull(id: RegistryId): T?

    /** Registers or replaces an entry. */
    operator fun <V : T> set(key: RegistryKey<V>, value: V)

    /** Registers or replaces an entry by ID. */
    operator fun set(id: RegistryId, value: T)

    /** Checks whether an entry exists. */
    operator fun contains(key: RegistryKey<*>): Boolean

    /** Checks whether an entry exists. */
    operator fun contains(id: RegistryId): Boolean

    /** Removes an entry. */
    fun unregister(key: RegistryKey<*>): Boolean

    /** Removes an entry by ID. */
    fun unregister(id: RegistryId): Boolean

    /** Replaces an existing entry. */
    fun replace(id: RegistryId, value: T): Boolean

    /** Removes all entries. */
    fun clear()

    /** Checks whether the registry is empty. */
    fun isEmpty(): Boolean

    /** Returns all registered identifiers. */
    fun ids(): Set<RegistryId>

    /** Returns an immutable snapshot. */
    fun snapshot(): Map<RegistryId, T>
}