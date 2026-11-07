package io.github.kiwiwhale.kiwiregistry.exception

import io.github.kiwiwhale.kiwiregistry.core.RegistryId
import io.github.kiwiwhale.kiwiregistry.core.RegistryGraph

/**
 * Thrown when a requested registry does not exist in a [RegistryGraph].
 *
 * @param id ID of the registry that could not be found.
 */
class RegistryNotFoundException(
    id: RegistryId
) : RegistryException(
    "Registry not found: $id"
)
