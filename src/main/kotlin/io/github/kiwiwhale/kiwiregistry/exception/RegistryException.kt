package io.github.kiwiwhale.kiwiregistry.exception

open class RegistryException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

class DuplicateRegistryEntryException(
    registryId: String,
    key: Any?
) : RegistryException(
    "Duplicate registry entry in '$registryId': $key"
)

class RegistryNotFoundException(
    registryId: String
) : RegistryException(
    "Registry not found: $registryId"
)