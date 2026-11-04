package io.github.kiwiwhale.kiwiregistry.exception

import io.github.kiwiwhale.kiwiregistry.bootstrap.RegistryHolder

open class BootstrapException(
    message: String,
    cause: Throwable? = null
) : RegistryException(message, cause)

class BootstrapAlreadyCompletedException :
    BootstrapException("Bootstrap has already been completed")

class DuplicateContributionException(
    holder: RegistryHolder
) : BootstrapException(
    "Contribution from holder ${holder::class.qualifiedName} is already registered"
)

class DuplicateRegistryEntryException(
    registryId: String,
    key: Any?
) : RegistryException(
    "Duplicate registry entry: $registryId / $key"
)

class InvalidContributionException(
    message: String
) : BootstrapException(message)
