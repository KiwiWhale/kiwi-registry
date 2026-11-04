package io.github.kiwiwhale.kiwiregistry

/**
 * Identifies a typed registry entry.
 *
 * @param T Registered value type.
 */
interface RegistryKey<T : Any> {

    val id: RegistryId
}
