package io.github.kiwiwhale.kiwiregistry.exception

import io.github.kiwiwhale.kiwiregistry.core.RegistryId

class RegistryTypeMismatchException(
    id: RegistryId
) : RegistryException(
    "Registry type mismatch: $id"
)
