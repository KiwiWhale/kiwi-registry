package io.github.kiwiwhale.kiwiregistry.exception

class RegistryBuildException(
    message: String,
    cause: Throwable? = null
) : RegistryException(message, cause)
