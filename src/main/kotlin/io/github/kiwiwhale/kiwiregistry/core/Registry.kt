package io.github.kiwiwhale.kiwiregistry.core

import io.github.kiwiwhale.kiwiregistry.bootstrap.RegistryHolder

interface Registry<K, V> {

    val registryKey: RegistryKey<K, V>

    val data: Map<K, V>

    val holderData: Map<RegistryHolder, Map<K, V>>

    val size: Int
        get() = data.size

    val isEmpty: Boolean
        get() = size == 0

    val isNotEmpty: Boolean
        get() = !isEmpty

    val keys: Set<K>
        get() = data.keys

    val values: Collection<V>
        get() = data.values

    operator fun get(key: K): V? =
        data[key]

    operator fun contains(key: K): Boolean =
        data.containsKey(key)

    fun all(): Map<K, V> =
        data

    fun getHolderData(holder: RegistryHolder): Map<K, V> =
        holderData[holder] ?: emptyMap()

    fun containsHolder(holder: RegistryHolder): Boolean =
        holderData.containsKey(holder)
}