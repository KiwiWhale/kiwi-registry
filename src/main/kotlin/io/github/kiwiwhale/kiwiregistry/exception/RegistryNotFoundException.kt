package io.github.kiwiwhale.kiwiregistry.exception

import io.github.kiwiwhale.kiwiregistry.core.RegistryId

class RegistryNotFoundException(
    id: RegistryId
) : RegistryException(
    "Registry not found: $id"
)
