package io.github.kiwiwhale.kiwiregistry.exception

import io.github.kiwiwhale.kiwiregistry.RegistryId

class RegistryNotFoundException(
    val registryId: RegistryId
) : RegistryException(
    "Registry not found: '$registryId'."
)
