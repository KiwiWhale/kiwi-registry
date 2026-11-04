package io.github.kiwiwhale.kiwiregistry.exception

open class BootstrapException(
    message: String,
    cause: Throwable? = null
) : RegistryException(message, cause)

class BootstrapAlreadyCompletedException :
    BootstrapException(
        "Bootstrap has already been completed."
    )

class BootstrapNotCompletedException :
    BootstrapException(
        "Bootstrap has not been completed yet."
    )

class BootstrapAlreadyInjectedException :
    BootstrapException(
        "BootstrapHolder has already been injected."
    )

class DuplicateContributionException(
    holderName: String
) : BootstrapException(
    "Contribution from holder '$holderName' is already registered."
)

class InvalidContributionException(
    message: String,
    cause: Throwable? = null
) : BootstrapException(message, cause)

class BootstrapNotInjectedException :
    BootstrapException(
        "BootstrapHolder has not been injected yet."
    )
