package io.github.kiwiwhale.kiwiregistry

/**
 * Typed identifier for a registry entry.
 *
 * Implementations must be immutable.
 *
 * Equality and hashCode should consistently represent the ID.
 *
 * In particular, keys with different IDs must not compare equal.
 *
 * @param T Registered value type.
 */
interface RegistryKey<T : Any> {

    val id: RegistryId
}
