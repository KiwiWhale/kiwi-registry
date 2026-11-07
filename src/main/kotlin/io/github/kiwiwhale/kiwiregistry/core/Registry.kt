package io.github.kiwiwhale.kiwiregistry.core

/**
 * Immutable registry containing key-value entries.
 */
interface Registry<K : Any, V : Any> {

    val key: RegistryKey<K, V>

    val data: Map<K, V>

    val size: Int
        get() = data.size

    val isEmpty: Boolean
        get() = data.isEmpty()

    val isNotEmpty: Boolean
        get() = !isEmpty

    val keys: Set<K>
        get() = data.keys

    val values: Collection<V>
        get() = data.values

    operator fun get(key: K): V? =
        data[key]

    operator fun contains(key: K): Boolean =
        key in data

    fun getOrNull(key: K): V? =
        get(key)

    fun getOrDefault(key: K, default: V): V =
        get(key) ?: default

    fun all(): Map<K, V> =
        data

    fun forEach(action: (K, V) -> Unit) {
        data.forEach(action)
    }
}
