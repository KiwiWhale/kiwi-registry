package io.github.kiwiwhale.kiwiregistry.validation

import io.github.kiwiwhale.kiwiregistry.core.RegistryId

/**
 * Represents an issue discovered during registry validation.
 *
 * @property severity Severity of this issue.
 * @property message Description of the issue.
 * @property registryId Optional ID of the related registry.
 */
data class ValidationIssue(
    val severity: ValidationSeverity,
    val message: String,
    val registryId: RegistryId? = null
) {

    init {
        require(message.isNotBlank()) {
            "Validation issue message cannot be blank."
        }
    }
}
