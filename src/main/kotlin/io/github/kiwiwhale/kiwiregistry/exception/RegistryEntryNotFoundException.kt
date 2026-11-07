package io.github.kiwiwhale.kiwiregistry.exception

import io.github.kiwiwhale.kiwiregistry.core.RegistryId

/**
 * Thrown when an entry cannot be found in a registry.
 *
 * @param registryId ID of the registry containing the entry.
 * @param entryKey Key of the missing entry.
 */
class RegistryEntryNotFoundException(
    val registryId: RegistryId,
    val entryKey: Any
) : RegistryException(
    "Entry '$entryKey' not found in registry '$registryId'."
)
