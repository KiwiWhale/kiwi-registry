package io.github.kiwiwhale.kiwiregistry.exception

open class RegistryException(
    message: String,
    cause: Throwable? = null
) : IllegalArgumentException(message, cause)
