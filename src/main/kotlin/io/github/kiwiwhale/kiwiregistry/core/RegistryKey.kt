package io.github.kiwiwhale.kiwiregistry.core

import kotlin.reflect.KClass

/**
 * Typed identifier for a registry.
 *
 * Equality and hash code are based exclusively on [id].
 * Runtime compatibility is additionally checked using key and value types.
 *
 * Runtime type checks use [KClass] and therefore cannot distinguish
 * parameterized generic types due to JVM type erasure.
 *
 * For example, List<String> and List<Int> are treated as the same
 * runtime type.
 *
 * Primitive JVM types are not supported.
 *
 * @param K Registry key type.
 * @param V Registry value type.
 * @property id Unique registry identifier.
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

    /**
     * Checks whether another registry key has identical runtime types.
     *
     * Generic type arguments are not checked because of JVM type erasure.
     * Registry IDs are intentionally not considered by this check.
     *
     * @param other Registry key to compare.
     * @return True if both raw key and value classes match.
     */
    internal fun hasSameTypes(other: RegistryKey<*, *>): Boolean =
        key == other.key && value == other.value

    /**
     * Compares registry keys by their registry IDs.
     *
     * Runtime types are not considered.
     *
     * @param other Object to compare.
     * @return True if both objects are registry keys with equal IDs.
     */
    override fun equals(other: Any?): Boolean =
        this === other || (other is RegistryKey<*, *> && id == other.id)

    /**
     * Returns the hash code of this registry ID.
     */
    override fun hashCode(): Int =
        id.hashCode()

    /**
     * Returns a readable representation of this registry key.
     */
    override fun toString(): String =
        "RegistryKey(id='$id')"
}

/**
 * Creates a typed registry key from a namespace and key name.
 *
 * Runtime types are inferred from the reified type parameters.
 * Only raw runtime classes are retained; generic type arguments
 * cannot be checked at runtime.
 *
 * @param namespace Registry namespace.
 * @param key Registry name within the namespace.
 * @return Typed registry key.
 */
inline fun <reified K : Any, reified V : Any> registryKey(
    namespace: String,
    key: String
): RegistryKey<K, V> =
    RegistryKey(
        RegistryId(namespace, key),
        K::class,
        V::class
    )

/**
 * Creates a typed registry key from an existing [RegistryId].
 *
 * Runtime types are inferred from the reified type parameters.
 * Only raw runtime classes are retained; generic type arguments
 * cannot be checked at runtime.
 *
 * @param id Registry identifier.
 * @return Typed registry key.
 */
inline fun <reified K : Any, reified V : Any> registryKey(
    id: RegistryId
): RegistryKey<K, V> =
    RegistryKey(
        id,
        K::class,
        V::class
    )
