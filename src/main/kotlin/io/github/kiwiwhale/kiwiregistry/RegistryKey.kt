package io.github.kiwiwhale.kiwiregistry

/**
 * Identifies a typed registry entry.
 *
 * @param T Type of the registered value.
 */
interface RegistryKey<out T : Any> {

    /**
     * Unique identifier of this entry.
     */
    val id: RegistryId
}