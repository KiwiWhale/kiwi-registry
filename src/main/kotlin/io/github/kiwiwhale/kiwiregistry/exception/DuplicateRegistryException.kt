package io.github.kiwiwhale.kiwiregistry.exception

import io.github.kiwiwhale.kiwiregistry.RegistryId

class DuplicateRegistryException(
    val registryId: RegistryId
) : RegistryException(
    "Registry already exists: '$registryId'."
)
