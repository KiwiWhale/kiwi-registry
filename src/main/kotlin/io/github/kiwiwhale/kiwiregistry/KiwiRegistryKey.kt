package io.github.kiwiwhale.kiwiregistry

/**
 * Default implementation of [RegistryKey].
 *
 * @param T Registered value type.
 * @property id Unique registry identifier.
 */
data class KiwiRegistryKey<T : Any>(
    override val id: RegistryId
) : RegistryKey<T> {

    companion object {

        /**
         * Creates a key from a namespace and entry name.
         */
        fun <T : Any> of(namespace: String, key: String): KiwiRegistryKey<T> =
            KiwiRegistryKey(RegistryId(namespace, key))
    }
}
