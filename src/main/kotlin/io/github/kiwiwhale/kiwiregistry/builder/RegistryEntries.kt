package io.github.kiwiwhale.kiwiregistry.builder

import io.github.kiwiwhale.kiwiregistry.core.RegistryKey
import io.github.kiwiwhale.kiwiregistry.exception.RegistryBuildException
import io.github.kiwiwhale.kiwiregistry.exception.RegistryEntryNotFoundException

/**
 * Mutable configuration scope for registry entries.
 *
 * This scope is valid only while its associated registry
 * configuration block is executing.
 *
 * Changes are committed by [RegistryBuilder] only when the
 * configuration block completes successfully.
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
     * All subsequent operations are rejected.
     */
    internal fun close() {
        active = false
    }

    /**
     * Ensures that this scope is still active.
     *
     * @throws IllegalStateException If the scope has been closed.
     */
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
    private fun validateEntry(
        entryKey: K,
        value: V
    ) {
        require(key.key.isInstance(entryKey)) {
            "Invalid key type in registry '${key.id}'."
        }

        require(key.value.isInstance(value)) {
            "Invalid value type in registry '${key.id}'."
        }
    }

    /**
     * Checks whether an entry key already exists.
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
     * Adds an entry.
     *
     * Duplicate keys are not allowed.
     *
     * @param key Entry key.
     * @param value Entry value.
     * @throws RegistryBuildException If the key already exists.
     * @throws IllegalArgumentException If the runtime types are incompatible.
     */
    fun add(
        key: K,
        value: V
    ) {
        checkActive()
        validateEntry(key, value)
        checkDuplicate(key)

        entries[key] = value
    }

    /**
     * Adds multiple entries.
     *
     * All entries are validated before any modification is made.
     *
     * Duplicate keys against existing entries are rejected.
     *
     * @param entries Entries to add.
     * @throws RegistryBuildException If a duplicate key exists.
     * @throws IllegalArgumentException If an entry has an incompatible type.
     */
    fun addAll(entries: Map<K, V>) {
        checkActive()

        val pending = LinkedHashMap<K, V>()

        for ((entryKey, value) in entries) {
            validateEntry(entryKey, value)
            checkDuplicate(entryKey)

            pending[entryKey] = value
        }

        for ((entryKey, value) in pending) {
            this.entries[entryKey] = value
        }
    }

    /**
     * Checks whether an entry with the specified key exists.
     */
    fun contains(key: K): Boolean {
        checkActive()
        return key in entries
    }

    /**
     * Checks whether the specified value exists.
     */
    fun containsValue(value: V): Boolean {
        checkActive()
        return entries.containsValue(value)
    }

    /**
     * Removes an entry.
     *
     * @return True if an entry was removed.
     */
    fun remove(key: K): Boolean {
        checkActive()

        if (!entries.containsKey(key)) {
            return false
        }

        entries.remove(key)
        return true
    }

    /**
     * Replaces an existing entry.
     *
     * The specified key must already exist.
     *
     * @return True if the entry was replaced, otherwise false.
     */
    fun replace(
        key: K,
        value: V
    ): Boolean {
        checkActive()
        validateEntry(key, value)

        if (!entries.containsKey(key)) {
            return false
        }

        entries[key] = value
        return true
    }

    /**
     * Returns an entry, or null if absent.
     */
    @Suppress("UNCHECKED_CAST")
    fun getOrNull(key: K): V? {
        checkActive()
        return entries[key] as V?
    }

    /**
     * Returns an entry.
     *
     * @throws RegistryEntryNotFoundException If the entry does not exist.
     */
    @Suppress("UNCHECKED_CAST")
    fun getOrThrow(key: K): V {
        checkActive()

        if (!entries.containsKey(key)) {
            throw RegistryEntryNotFoundException(
                this.key.id,
                key
            )
        }

        return entries[key] as V
    }

    /**
     * Removes all entries.
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
     * Number of configured entries.
     */
    val size: Int
        get() {
            checkActive()
            return entries.size
        }

    /**
     * Whether this scope contains no entries.
     */
    val isEmpty: Boolean
        get() {
            checkActive()
            return entries.isEmpty()
        }

    /**
     * Whether this scope contains at least one entry.
     */
    val isNotEmpty: Boolean
        get() = !isEmpty
}
