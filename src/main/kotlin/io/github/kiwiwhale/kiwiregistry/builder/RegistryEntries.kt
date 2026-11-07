package io.github.kiwiwhale.kiwiregistry.builder

import io.github.kiwiwhale.kiwiregistry.core.RegistryKey
import io.github.kiwiwhale.kiwiregistry.exception.RegistryBuildException
import io.github.kiwiwhale.kiwiregistry.exception.RegistryEntryNotFoundException

/**
 * Mutable entry configuration scope.
 *
 * This object is only valid during its registry configuration block.
 *
 * Entries are committed by [RegistryBuilder] only after successful
 * completion of the configuration block.
 *
 * This class is not thread-safe.
 *
 * @param K Registry key type.
 * @param V Registry value type.
 */
class RegistryEntries<K : Any, V : Any> internal constructor(
    private val key: RegistryKey<K, V>,
    private val entries: MutableMap<Any, Any>,
    private val contributor: String
) {

    private var active = true

    /**
     * Closes this configuration scope.
     *
     * Further operations on this instance are rejected.
     */
    internal fun close() {
        active = false
    }

    private fun checkActive() {
        check(active) {
            "RegistryEntries is no longer active."
        }
    }

    /**
     * Validates the runtime types of an entry.
     *
     * @throws IllegalArgumentException If either type is incompatible.
     */
    private fun validateEntry(entryKey: K, value: V) {
        require(key.key.isInstance(entryKey)) {
            "Invalid key type in registry '${key.id}'."
        }

        require(key.value.isInstance(value)) {
            "Invalid value type in registry '${key.id}'."
        }
    }

    /**
     * Throws if the specified key already exists.
     *
     * @throws RegistryBuildException If the key already exists.
     */
    private fun checkDuplicate(entryKey: K) {
        if (entries.containsKey(entryKey)) {
            throw RegistryBuildException(
                "Duplicate entry '$entryKey' in registry '${key.id}' " +
                        "from contributor '$contributor'."
            )
        }
    }

    /**
     * Adds an entry to the registry.
     *
     * Duplicate keys are not allowed.
     */
    fun add(key: K, value: V) {
        checkActive()
        validateEntry(key, value)
        checkDuplicate(key)

        entries[key] = value
    }

    /**
     * Adds multiple entries to the registry.
     *
     * All entries are validated before any entry is inserted.
     *
     * Duplicate keys against existing entries or within the
     * supplied collection are rejected.
     *
     * @param entries Entries to add.
     * @throws RegistryBuildException If a duplicate key exists.
     * @throws IllegalArgumentException If an entry has an incompatible type.
     * @throws IllegalStateException If this scope is no longer active.
     */
    fun addAll(entries: Map<K, V>) {
        checkActive()

        val pending = LinkedHashMap<K, V>()

        for ((entryKey, value) in entries) {
            validateEntry(entryKey, value)
            checkDuplicate(entryKey)

            if (pending.containsKey(entryKey)) {
                throw RegistryBuildException(
                    "Duplicate entry '$entryKey' in registry '${key.id}' " +
                            "from contributor '$contributor'."
                )
            }

            pending[entryKey] = value
        }

        for ((entryKey, value) in pending) {
            this.entries[entryKey] = value
        }
    }

    /**
     * Checks whether the specified key exists.
     */
    fun contains(key: K): Boolean {
        checkActive()
        return entries.containsKey(key)
    }

    /**
     * Checks whether the specified value exists.
     */
    fun containsValue(value: V): Boolean {
        checkActive()
        return entries.containsValue(value)
    }

    /**
     * Removes an entry by its key.
     *
     * @return True if an entry was removed.
     */
    fun remove(key: K): Boolean {
        checkActive()
        return entries.remove(key) != null
    }

    /**
     * Replaces an existing entry.
     *
     * The key must already exist.
     *
     * @return True if the entry was replaced.
     */
    fun replace(key: K, value: V): Boolean {
        checkActive()
        validateEntry(key, value)

        if (!entries.containsKey(key)) {
            return false
        }

        entries[key] = value
        return true
    }

    /**
     * Returns an entry by its key, or null if absent.
     */
    @Suppress("UNCHECKED_CAST")
    fun getOrNull(key: K): V? {
        checkActive()
        return entries[key] as V?
    }

    /**
     * Returns an entry by its key.
     *
     * @throws NoSuchElementException If the key does not exist.
     */
    fun getOrThrow(key: K): V {
        checkActive()

        @Suppress("UNCHECKED_CAST")
        return entries[key] as V?
            ?: throw RegistryEntryNotFoundException(this.key.id, key)
    }

    /**
     * Removes all configured entries.
     *
     * @return Number of removed entries.
     */
    fun clear(): Int {
        checkActive()

        val removedCount = entries.size
        entries.clear()

        return removedCount
    }

    /**
     * Number of entries currently configured.
     */
    val size: Int
        get() {
            checkActive()
            return entries.size
        }

    /**
     * Whether this registry currently contains no entries.
     */
    val isEmpty: Boolean
        get() {
            checkActive()
            return entries.isEmpty()
        }

    /**
     * Whether this registry currently contains at least one entry.
     */
    val isNotEmpty: Boolean
        get() = !isEmpty
}
