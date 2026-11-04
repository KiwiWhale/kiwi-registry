package io.github.kiwiwhale.kiwiregistry.exception

import io.github.kiwiwhale.kiwiregistry.RegistryId

class RegistryFrozenException(
    val registryId: RegistryId
) : RegistryException(
    "Registry is frozen: '$registryId'."
)
