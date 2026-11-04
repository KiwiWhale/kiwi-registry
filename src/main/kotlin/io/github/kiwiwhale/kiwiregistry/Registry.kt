package io.github.kiwiwhale.kiwiregistry

interface Registry<T : Any, K : RegistryKey> {

    val size: Int

    fun register(key: K, value: T)

    fun register(name: String, value: T)

    fun registerAll(additions: Map<K, T>)

    fun get(key: K): T

    fun get(name: String): T

    fun getOrNull(key: K): T?

    fun getOrNull(name: String): T?

    fun contains(key: K): Boolean

    fun contains(name: String): Boolean

    fun unregister(key: K): Boolean

    fun unregister(name: String): Boolean

    fun keys(): Set<K>

    fun names(): Set<String>

    fun snapshot(): Map<K, T>
}
