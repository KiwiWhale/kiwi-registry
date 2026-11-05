package io.github.kiwiwhale.kiwiregistry.core

interface RegistryKey<K, V> {

    val id: RegistryId

    companion object {
        fun <K, V> simple(id: RegistryId): RegistryKey<K, V> =
            SimpleRegistryKey(id)

        fun <K, V> simple(id: String): RegistryKey<K, V> =
            SimpleRegistryKey(RegistryId.parse(id))
    }
}

data class SimpleRegistryKey<K, V>(
    override val id: RegistryId
) : RegistryKey<K, V>
