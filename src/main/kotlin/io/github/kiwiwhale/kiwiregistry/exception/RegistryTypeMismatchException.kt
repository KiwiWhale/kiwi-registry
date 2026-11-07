package io.github.kiwiwhale.kiwiregistry.exception

import io.github.kiwiwhale.kiwiregistry.core.RegistryId

/**
 * Thrown when a registry ID exists but its runtime types
 * do not match the requested registry key.
 *
 * @param id ID of the incompatible registry.
 */
class RegistryTypeMismatchException(
    id: RegistryId
) : RegistryException(
    "Registry type mismatch: $id"
)
