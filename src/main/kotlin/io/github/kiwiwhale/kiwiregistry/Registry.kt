package io.github.kiwiwhale.kiwiregistry

/**
 * Contract for managing registry entries.
 *
 * Supports typed keys and RegistryId-based access.
 *
 * @param T Registered value type.
 * @param K Registry key type.
 */
interface Registry<T : Any, K : RegistryKey<T>> {

    /**
     * Number of registered entries.
     */
    val size: Int

    /**
     * Registers an entry.
     *
     * @throws DuplicateRegistryEntryException If the ID already exists.
     */
    fun register(key: K, value: T)

    /**
     * Registers an entry using its ID.
     *
     * @throws DuplicateRegistryEntryException If the ID already exists.
     */
    fun register(id: RegistryId, value: T)

    /**
     * Registers multiple entries atomically.
     */
    fun registerAll(additions: Map<K, T>)

    /**
     * Registers an entry only if its ID does not exist.
     *
     * @return true if registered, false if already present.
     */
    fun registerIfAbsent(key: K, value: T): Boolean

    /**
     * Registers an entry only if its ID does not exist.
     */
    fun registerIfAbsent(id: RegistryId, value: T): Boolean

    /**
     * Retrieves an entry by typed key.
     *
     * @throws RegistryEntryNotFoundException If not found.
     */
    operator fun get(key: K): T

    /**
     * Retrieves an entry by ID.
     */
    operator fun get(id: RegistryId): T

    /**
     * Retrieves an entry or returns null.
     */
    fun getOrNull(key: K): T?

    /**
     * Retrieves an entry or returns null.
     */
    fun getOrNull(id: RegistryId): T?

    /**
     * Registers or replaces an entry.
     */
    operator fun set(key: K, value: T)

    /**
     * Registers or replaces an entry by ID.
     */
    operator fun set(id: RegistryId, value: T)

    /**
     * Checks whether an entry exists.
     */
    operator fun contains(key: K): Boolean

    /**
     * Checks whether an entry exists.
     */
    operator fun contains(id: RegistryId): Boolean

    /**
     * Removes an entry.
     */
    fun unregister(key: K): Boolean

    /**
     * Removes an entry by ID.
     */
    fun unregister(id: RegistryId): Boolean

    /**
     * Replaces an existing entry.
     *
     * @return true if an entry was replaced.
     */
    fun replace(key: K, value: T): Boolean

    /**
     * Replaces an existing entry by ID.
     */
    fun replace(id: RegistryId, value: T): Boolean

    /**
     * Removes all entries.
     */
    fun clear()

    /**
     * Checks whether the registry is empty.
     */
    fun isEmpty(): Boolean

    /**
     * Returns all registered keys.
     */
    fun keys(): Set<K>

    /**
     * Returns all registered IDs.
     */
    fun ids(): Set<RegistryId>

    /**
     * Returns an immutable snapshot.
     */
    fun snapshot(): Map<K, T>
}