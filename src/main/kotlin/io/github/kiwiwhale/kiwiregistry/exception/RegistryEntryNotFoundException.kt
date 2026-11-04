package io.github.kiwiwhale.kiwiregistry.exception

/**
 * Thrown when a requested registry entry does not exist.
 */
class RegistryEntryNotFoundException(
    val registryId: String
) : NoSuchElementException("Registry entry not found: '$registryId'.")
