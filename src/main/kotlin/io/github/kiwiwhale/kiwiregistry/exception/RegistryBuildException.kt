package io.github.kiwiwhale.kiwiregistry.exception

/**
 * Thrown when a registry or registry graph cannot be built.
 *
 * This exception may wrap an underlying cause.
 *
 * @param message Description of the build failure.
 * @param cause Original cause of the failure, if available.
 */
class RegistryBuildException(
    message: String,
    cause: Throwable? = null
) : RegistryException(message, cause)

