package io.github.kiwiwhale.kiwiregistry.core

/**
 * Immutable registry containing key-value entries.
 *
 * The registry structure cannot be modified after construction.
 * However, immutability of individual keys and values depends
 * on their respective implementations.
 *
 * Implementations must expose an immutable map through [data].
 *
 * @param K Registry key type.
 * @param V Registry value type.
 */
interface Registry<K : Any, V : Any> {

    /**
     * Typed identifier of this registry.
     */
    val key: RegistryKey<K, V>

    /**
     * Immutable map containing all registry entries.
     */
    val data: Map<K, V>

    /**
     * Number of entries in this registry.
     */
    val size: Int
        get() = data.size

    /**
     * Whether this registry contains no entries.
     */
    val isEmpty: Boolean
        get() = data.isEmpty()

    /**
     * Whether this registry contains at least one entry.
     */
    val isNotEmpty: Boolean
        get() = !isEmpty

    /**
     * All keys contained in this registry.
     */
    val keys: Set<K>
        get() = data.keys

    /**
     * All values contained in this registry.
     */
    val values: Collection<V>
        get() = data.values

    /**
     * Returns the value associated with [key], or null if absent.
     *
     * @param key Entry key.
     * @return Associated value, or null.
     */
    operator fun get(key: K): V? =
        data[key]

    /**
     * Checks whether [key] exists in this registry.
     *
     * @param key Entry key.
     * @return True if the key exists.
     */
    operator fun contains(key: K): Boolean =
        key in data

    /**
     * Returns the value associated with [key], or null if absent.
     *
     * This is equivalent to the indexed [get] operator.
     *
     * @param key Entry key.
     * @return Associated value, or null.
     */
    fun getOrNull(key: K): V? =
        get(key)

    /**
     * Returns the value associated with [key], or [default]
     * if the key is absent.
     *
     * @param key Entry key.
     * @param default Value returned when the key is absent.
     * @return Associated value or [default].
     */
    fun getOrDefault(key: K, default: V): V =
        get(key) ?: default

    /**
     * Returns all registry entries as an immutable map.
     *
     * @return Registry data.
     */
    fun all(): Map<K, V> =
        data

    /**
     * Performs [action] for each registry entry in iteration order.
     *
     * @param action Operation to perform for each key-value pair.
     */
    fun forEach(action: (K, V) -> Unit) {
        data.forEach(action)
    }
}
