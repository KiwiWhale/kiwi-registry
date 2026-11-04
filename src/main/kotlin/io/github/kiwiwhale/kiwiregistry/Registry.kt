package io.github.kiwiwhale.kiwiregistry

interface Registry<T : Any> : Iterable<T> {

    val id: RegistryId

    val size: Int

    val isEmpty: Boolean
        get() = size == 0

    operator fun get(key: String): T

    fun getOrNull(key: String): T?

    operator fun contains(key: String): Boolean

    fun keys(): Set<String>

    fun values(): Collection<T>

    fun entries(): Set<Map.Entry<String, T>>

    fun snapshot(): Map<String, T>

    override fun iterator(): Iterator<T> =
        values().iterator()

    operator fun get(key: RegistryKey<T>): T {
        return get(key.id.key)
    }

    fun getOrNull(key: RegistryKey<T>): T? {
        return getOrNull(key.id.key)
    }

    operator fun contains(key: RegistryKey<T>): Boolean {
        return key.id.key in this
    }
}