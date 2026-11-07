package io.github.kiwiwhale.kiwiregistry.validation

/**
 * Severity of a registry validation issue.
 *
 * ERROR prevents RegistryGraph construction.
 * WARNING and INFO do not prevent construction.
 */
enum class ValidationSeverity {
    ERROR,
    WARNING,
    INFO
}
