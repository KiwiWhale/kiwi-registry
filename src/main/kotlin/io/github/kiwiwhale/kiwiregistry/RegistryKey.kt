package io.github.kiwiwhale.kiwiregistry

import kotlin.reflect.KClass

data class RegistryKey<T : Any>(
    val id: RegistryId,
    val type: KClass<T>
) {
    companion object {
        inline fun <reified T : Any> of(
            namespace: String,
            key: String
        ): RegistryKey<T> =
            RegistryKey(
                RegistryId(namespace, key),
                T::class
            )

        fun <T : Any> of(
            namespace: String,
            key: String,
            type: KClass<T>
        ): RegistryKey<T> =
            RegistryKey(
                RegistryId(namespace, key),
                type
            )

        fun <T : Any> of(
            id: RegistryId,
            type: KClass<T>
        ): RegistryKey<T> =
            RegistryKey(id, type)
    }
}