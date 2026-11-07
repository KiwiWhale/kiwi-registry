package io.github.kiwiwhale.kiwiregistry.dsl

import io.github.kiwiwhale.kiwiregistry.core.Registry
import io.github.kiwiwhale.kiwiregistry.core.RegistryId
import io.github.kiwiwhale.kiwiregistry.core.RegistryKey
import io.github.kiwiwhale.kiwiregistry.validation.RegistryValidator

/**
 * Provides a DSL for validating registry configuration.
 *
 * Errors prevent registry graph construction, while warnings
 * and informational issues do not.
 *
 * @property validator Validator used to access registries and collect issues.
 */
@RegistryDslMarker
class RegistryValidationScope internal constructor(
    private val validator: RegistryValidator
) {

    /**
     * Reports a validation error.
     */
    fun error(
        message: String,
        registryId: RegistryId? = null
    ) = validator.error(message, registryId)

    /**
     * Reports a validation warning.
     */
    fun warning(
        message: String,
        registryId: RegistryId? = null
    ) = validator.warning(message, registryId)

    /**
     * Reports an informational validation issue.
     */
    fun info(
        message: String,
        registryId: RegistryId? = null
    ) = validator.info(message, registryId)

    /**
     * Reports an error when the condition is false.
     *
     * @return Whether the condition is satisfied.
     */
    fun requireCondition(
        condition: Boolean,
        message: String,
        registryId: RegistryId? = null
    ): Boolean {
        if (!condition) {
            error(message, registryId)
        }

        return condition
    }

    /**
     * Checks whether a registry with the specified ID exists.
     */
    fun containsRegistry(id: RegistryId): Boolean =
        validator.containsRegistry(id)

    /**
     * Checks whether a compatible registry exists.
     */
    fun containsRegistry(key: RegistryKey<*, *>): Boolean =
        validator.containsRegistry(key)

    /**
     * Returns a registry if it exists and has compatible runtime types.
     */
    fun <K : Any, V : Any> getRegistry(
        key: RegistryKey<K, V>
    ): Registry<K, V>? =
        validator.getOrNull(key)

    /**
     * Requires a registry to exist.
     *
     * @param key Registry key.
     * @param message Error message when the registry is missing.
     * @return The registry, or null when it is missing or incompatible.
     */
    fun <K : Any, V : Any> requireRegistry(
        key: RegistryKey<K, V>,
        message: String = "Required registry '${key.id}' is missing."
    ): Registry<K, V>? {
        return getRegistry(key) ?: run {
            error(message, key.id)
            null
        }
    }

    /**
     * Returns an entry if its registry and key exist.
     */
    fun <K : Any, V : Any> getEntry(
        key: RegistryKey<K, V>,
        entryKey: K
    ): V? =
        getRegistry(key)?.getOrNull(entryKey)

    /**
     * Requires an entry to exist.
     *
     * @param key Registry containing the entry.
     * @param entryKey Entry key.
     * @param message Error message when the entry is missing.
     * @return The entry, or null when it is missing.
     */
    fun <K : Any, V : Any> requireEntry(
        key: RegistryKey<K, V>,
        entryKey: K,
        message: String =
            "Required entry '$entryKey' is missing from registry '${key.id}'."
    ): V? {
        val registry = requireRegistry(key) ?: return null

        return registry.getOrNull(entryKey) ?: run {
            error(message, key.id)
            null
        }
    }

    /**
     * Requires a registry to contain at least one entry.
     *
     * @return Whether the registry exists and is not empty.
     */
    fun <K : Any, V : Any> requireNotEmpty(
        key: RegistryKey<K, V>,
        message: String = "Registry '${key.id}' must not be empty."
    ): Boolean {
        val registry = requireRegistry(key) ?: return false

        return requireCondition(
            condition = registry.data.isNotEmpty(),
            message = message,
            registryId = key.id
        )
    }

    /**
     * Validates an existing registry using a custom predicate.
     *
     * @param key Registry to validate.
     * @param message Error message when validation fails.
     * @param predicate Validation condition.
     * @return Whether the registry satisfies the predicate.
     */
    fun <K : Any, V : Any> validateRegistry(
        key: RegistryKey<K, V>,
        message: String = "Registry '${key.id}' failed validation.",
        predicate: (Map<K, V>) -> Boolean
    ): Boolean {
        val registry = requireRegistry(key) ?: return false

        return requireCondition(
            condition = predicate(registry.data),
            message = message,
            registryId = key.id
        )
    }

    /**
     * Validates an existing entry using a custom predicate.
     *
     * @param key Registry containing the entry.
     * @param entryKey Entry key to validate.
     * @param message Error message when validation fails.
     * @param predicate Validation condition.
     * @return Whether the entry satisfies the predicate.
     */
    fun <K : Any, V : Any> validateEntry(
        key: RegistryKey<K, V>,
        entryKey: K,
        message: String =
            "Entry '$entryKey' in registry '${key.id}' failed validation.",
        predicate: (V) -> Boolean
    ): Boolean {
        val value = requireEntry(key, entryKey) ?: return false

        return requireCondition(
            condition = predicate(value),
            message = message,
            registryId = key.id
        )
    }

    /**
     * Validates every entry in a registry.
     *
     * All invalid entries are reported without stopping validation.
     *
     * @param key Registry containing the entries.
     * @param message Message generator for invalid entries.
     * @param predicate Validation condition.
     * @return Whether every entry satisfies the predicate.
     */
    fun <K : Any, V : Any> validateEntries(
        key: RegistryKey<K, V>,
        message: (K, V) -> String = { entryKey, _ ->
            "Entry '$entryKey' in registry '${key.id}' failed validation."
        },
        predicate: (K, V) -> Boolean
    ): Boolean {
        val registry = requireRegistry(key) ?: return false

        var valid = true

        for ((entryKey, value) in registry.data) {
            if (!predicate(entryKey, value)) {
                error(
                    message(entryKey, value),
                    key.id
                )
                valid = false
            }
        }

        return valid
    }
}
