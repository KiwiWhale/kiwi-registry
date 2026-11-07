package io.github.kiwiwhale.kiwiregistry.validation

import io.github.kiwiwhale.kiwiregistry.core.Registry
import io.github.kiwiwhale.kiwiregistry.core.RegistryGraph
import io.github.kiwiwhale.kiwiregistry.core.RegistryId
import io.github.kiwiwhale.kiwiregistry.core.RegistryKey
import io.github.kiwiwhale.kiwiregistry.dsl.RegistryValidationScope

/**
 * Validates registry configuration.
 *
 * Provides read-only access to configured registries
 * and collects validation issues.
 *
 * This class is not thread-safe.
 *
 * @param registries Registries to validate.
 */
class RegistryValidator internal constructor(
    private val registries: Map<RegistryId, Registry<*, *>>
) {

    internal constructor(graph: RegistryGraph) :
            this(graph.all().associateBy { it.key.id })

    private val issues = mutableListOf<ValidationIssue>()

    /**
     * Checks whether a registry exists.
     */
    fun containsRegistry(id: RegistryId): Boolean =
        id in registries

    /**
     * Checks whether a registry with the specified key exists.
     *
     * Runtime type compatibility is also checked.
     */
    fun containsRegistry(key: RegistryKey<*, *>): Boolean {
        val registry = registries[key.id] ?: return false

        return registry.key.hasSameTypes(key)
    }

    /**
     * Returns a registry by its ID.
     */
    fun getOrNull(id: RegistryId): Registry<*, *>? =
        registries[id]

    /**
     * Returns a registry by its typed key.
     *
     * Returns null if the registry does not exist
     * or its runtime types do not match.
     */
    @Suppress("UNCHECKED_CAST")
    fun <K : Any, V : Any> getOrNull(
        key: RegistryKey<K, V>
    ): Registry<K, V>? {
        val registry = registries[key.id] ?: return null

        if (!registry.key.hasSameTypes(key)) {
            return null
        }

        return registry as Registry<K, V>
    }

    /**
     * Reports a validation error.
     */
    fun error(
        message: String,
        registryId: RegistryId? = null
    ) {
        addIssue(
            ValidationSeverity.ERROR,
            message,
            registryId
        )
    }

    /**
     * Reports a validation warning.
     */
    fun warning(
        message: String,
        registryId: RegistryId? = null
    ) {
        addIssue(
            ValidationSeverity.WARNING,
            message,
            registryId
        )
    }

    /**
     * Reports an informational validation issue.
     */
    fun info(
        message: String,
        registryId: RegistryId? = null
    ) {
        addIssue(
            ValidationSeverity.INFO,
            message,
            registryId
        )
    }

    /**
     * Adds a validation issue.
     */
    private fun addIssue(
        severity: ValidationSeverity,
        message: String,
        registryId: RegistryId?
    ) {
        issues.add(
            ValidationIssue(
                severity = severity,
                message = message,
                registryId = registryId
            )
        )
    }

    /**
     * Executes validation rules.
     */
    internal fun validate(
        block: RegistryValidationScope.() -> Unit
    ) {
        RegistryValidationScope(this).block()
    }

    /**
     * Returns an immutable snapshot of validation issues.
     */
    fun result(): RegistryValidationResult =
        if (issues.isEmpty()) {
            RegistryValidationResult.VALID
        } else {
            RegistryValidationResult(issues)
        }
}
