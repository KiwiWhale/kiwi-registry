package io.github.kiwiwhale.kiwiregistry.exception

import io.github.kiwiwhale.kiwiregistry.validation.RegistryValidationResult

/**
 * Thrown when registry validation contains errors.
 *
 * Warnings and informational issues do not cause this exception.
 *
 * @property result Validation result containing all discovered issues.
 */
class RegistryValidationException(
    val result: RegistryValidationResult
) : RuntimeException(createMessage(result)) {

    init {
        require(result.hasErrors) {
            "RegistryValidationException requires at least one error."
        }
    }

    companion object {
        private fun createMessage(
            result: RegistryValidationResult
        ): String = buildString {
            append("Registry validation failed: ")
            append(result.errorCount)
            append(" error(s).")

            result.errors.forEachIndexed { index, issue ->
                appendLine()
                append("${index + 1}. ")
                append(issue.message)

                issue.registryId?.let {
                    append(" [Registry: ")
                    append(it)
                    append("]")
                }
            }
        }
    }
}
