package io.github.kiwiwhale.kiwiregistry.core

import io.github.kiwiwhale.kiwiregistry.exception.RegistryEntryNotFoundException

/**
 * Immutable registry containing typed key-value entries.
 *
 * Entries are exposed through a read-only map.
 * The registry preserves the insertion order of its entries.
 *
 * @param K Entry key type.
 * @param V Entry value type.
 */
interface Registry<K : Any, V : Any> {

    /**
     * Typed key identifying this registry.
     */
    val key: RegistryKey<K, V>

    /**
     * All registered entries.
     *
     * Implementations must expose a read-only map.
     */
    val data: Map<K, V>

    /**
     * Number of registered entries.
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
     * All registered entry keys.
     */
    val keys: Set<K>
        get() = data.keys

    /**
     * All registered entry values.
     */
    val values: Collection<V>
        get() = data.values

    /**
     * Returns the value associated with the specified key.
     *
     * @param key Entry key.
     * @return Associated value, or null if absent.
     */
    operator fun get(key: K): V? =
        data[key]

    /**
     * Checks whether the specified key exists.
     *
     * @param key Entry key.
     * @return True if the key exists.
     */
    operator fun contains(key: K): Boolean =
        key in data

    /**
     * Checks whether the specified value exists.
     *
     * @param value Entry value.
     * @return True if the value exists.
     */
    fun containsValue(value: V): Boolean =
        data.containsValue(value)

    /**
     * Returns the value associated with the specified key.
     *
     * @param key Entry key.
     * @return Associated value.
     * @throws RegistryEntryNotFoundException If the key does not exist.
     */
    fun getOrThrow(key: K): V =
        get(key) ?: throw RegistryEntryNotFoundException(
            this.key.id,
            key
        )

    /**
     * Returns the value associated with the specified key,
     * or null if absent.
     *
     * This is equivalent to [get].
     *
     * @param key Entry key.
     * @return Associated value, or null if absent.
     */
    fun getOrNull(key: K): V? =
        get(key)

    /**
     * Returns the value associated with the specified key,
     * or the specified default value if absent.
     *
     * @param key Entry key.
     * @param default Default value.
     * @return Associated value or default value.
     */
    fun getOrDefault(key: K, default: V): V =
        get(key) ?: default

    /**
     * Returns all registered entries.
     *
     * @return Read-only map of registered entries.
     */
    fun all(): Map<K, V> =
        data

    /**
     * Performs the specified action for each entry.
     *
     * Entries are visited in insertion order.
     *
     * @param action Operation performed for each key-value pair.
     */
    fun forEach(action: (K, V) -> Unit) {
        data.forEach(action)
    }
}
