package io.github.kiwiwhale.kiwiregistry.exception

/**
 * Base exception for Registry-related failures.
 *
 * All library-specific runtime exceptions should extend this class.
 *
 * @param message Description of the failure.
 * @param cause Original cause of the failure, if available.
 */
open class RegistryException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
