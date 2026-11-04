package io.github.kiwiwhale.kiwiregistry.exception

import io.github.kiwiwhale.kiwiregistry.RegistryId

class RegistryEntryNotFoundException(
    val registryId: RegistryId,
    val key: String
) : RegistryException(
    "Registry entry not found: '$registryId:$key'."
)
