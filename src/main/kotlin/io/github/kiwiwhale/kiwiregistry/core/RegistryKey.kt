package io.github.kiwiwhale.kiwiregistry.core

import kotlin.reflect.KClass

/**
 * Typed identifier for a registry.
 *
 * Equality is based on RegistryId.
 * Runtime compatibility is additionally checked using key and value types.
 */
class RegistryKey<K : Any, V : Any>(
    val id: RegistryId,
    internal val key: KClass<K>,
    internal val value: KClass<V>
) {

    init {
        require(!key.java.isPrimitive) {
            "Primitive key types are not supported."
        }

        require(!value.java.isPrimitive) {
            "Primitive value types are not supported."
        }
    }

    internal fun hasSameTypes(other: RegistryKey<*, *>): Boolean =
        key == other.key && value == other.value

    override fun equals(other: Any?): Boolean =
        this === other || (other is RegistryKey<*, *> && id == other.id)

    override fun hashCode(): Int =
        id.hashCode()

    override fun toString(): String =
        "RegistryKey(id='$id')"
}

inline fun <reified K : Any, reified V : Any> registryKey(
    namespace: String,
    key: String
): RegistryKey<K, V> =
    RegistryKey(
        RegistryId(namespace, key),
        K::class,
        V::class
    )

inline fun <reified K : Any, reified V : Any> registryKey(
    id: RegistryId
): RegistryKey<K, V> =
    RegistryKey(
        id,
        K::class,
        V::class
    )
