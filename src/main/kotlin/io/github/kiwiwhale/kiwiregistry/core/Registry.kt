package io.github.kiwiwhale.kiwiregistry.core

import io.github.kiwiwhale.kiwiregistry.bootstrap.RegistryHolder

interface Registry<K, V> {

    val registryKey: RegistryKey<K, V>

    val data: Map<K, V>

    val holderData: Map<RegistryHolder, Map<K, V>>

    val size: Int
        get() = data.size

    operator fun get(key: K): V? = data[key]

    fun all(): Map<K, V> = data

    fun withData(
        data: Map<K, V>,
        holderData: Map<RegistryHolder, Map<K, V>>
    ): Registry<K, V>
}
