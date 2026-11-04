package io.github.kiwiwhale.kiwiregistry.core

interface RegistryKey<K, V> {

    val id: RegistryId

    companion object {
        fun <K, V> simple(id: RegistryId): SimpleRegistryKey<K, V> =
            SimpleRegistryKey(id)
    }
}

data class SimpleRegistryKey<K, V>(
    override val id: RegistryId
) : RegistryKey<K, V>
