package io.github.kiwiwhale.kiwiregistry.validation

import java.util.*

/**
 * Immutable result of registry validation.
 *
 * Contains errors, warnings, and informational issues.
 *
 * Only ERROR issues make the validation result invalid.
 *
 * Issue discovery order is preserved.
 *
 * @param issues Validation issues.
 */
class RegistryValidationResult internal constructor(
    issues: Collection<ValidationIssue>
) {

    /**
     * Immutable snapshot of all validation issues.
     */
    val issues: List<ValidationIssue> =
        Collections.unmodifiableList(ArrayList(issues))

    /**
     * Validation errors.
     */
    val errors: List<ValidationIssue> =
        filterBySeverity(ValidationSeverity.ERROR)

    /**
     * Validation warnings.
     */
    val warnings: List<ValidationIssue> =
        filterBySeverity(ValidationSeverity.WARNING)

    /**
     * Informational validation issues.
     */
    val infos: List<ValidationIssue> =
        filterBySeverity(ValidationSeverity.INFO)

    /**
     * Number of validation errors.
     */
    val errorCount: Int
        get() = errors.size

    /**
     * Number of validation warnings.
     */
    val warningCount: Int
        get() = warnings.size

    /**
     * Number of informational issues.
     */
    val infoCount: Int
        get() = infos.size

    /**
     * Whether validation contains errors.
     */
    val hasErrors: Boolean
        get() = errorCount > 0

    /**
     * Whether validation contains warnings.
     */
    val hasWarnings: Boolean
        get() = warningCount > 0

    /**
     * Whether validation contains informational issues.
     */
    val hasInfos: Boolean
        get() = infoCount > 0

    /**
     * Whether validation passed without errors.
     */
    val isValid: Boolean
        get() = !hasErrors

    /**
     * Whether there are no validation issues.
     */
    val isEmpty: Boolean
        get() = issues.isEmpty()

    /**
     * Total number of validation issues.
     */
    val size: Int
        get() = issues.size

    private fun filterBySeverity(
        severity: ValidationSeverity
    ): List<ValidationIssue> =
        Collections.unmodifiableList(
            issues.filter { it.severity == severity }
        )

    override fun equals(other: Any?): Boolean =
        this === other ||
                (other is RegistryValidationResult &&
                        issues == other.issues)

    override fun hashCode(): Int =
        issues.hashCode()

    override fun toString(): String =
        "RegistryValidationResult(" +
                "errors=$errorCount, " +
                "warnings=$warningCount, " +
                "infos=$infoCount)"

    companion object {

        /**
         * Shared result representing successful validation.
         */
        val VALID = RegistryValidationResult(emptyList())
    }
}
