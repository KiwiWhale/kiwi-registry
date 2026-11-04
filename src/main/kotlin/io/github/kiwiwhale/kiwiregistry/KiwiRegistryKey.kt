package io.github.kiwiwhale.kiwiregistry

/**
 * Default immutable implementation of [RegistryKey].
 */
data class KiwiRegistryKey<T : Any>(
    override val id: RegistryId
) : RegistryKey<T> {

    companion object {

        /**
         * Creates a registry key.
         */
        @JvmStatic
        fun <T : Any> of(
            namespace: String,
            key: String
        ): KiwiRegistryKey<T> =
            KiwiRegistryKey(
                RegistryId(namespace, key)
            )
    }
}
