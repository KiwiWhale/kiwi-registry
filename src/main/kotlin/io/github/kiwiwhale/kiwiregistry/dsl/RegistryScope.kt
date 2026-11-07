package io.github.kiwiwhale.kiwiregistry.dsl

import io.github.kiwiwhale.kiwiregistry.builder.RegistryBuilder
import io.github.kiwiwhale.kiwiregistry.builder.RegistryContributor
import io.github.kiwiwhale.kiwiregistry.builder.RegistryEntries
import io.github.kiwiwhale.kiwiregistry.core.RegistryId
import io.github.kiwiwhale.kiwiregistry.core.RegistryKey

/**
 * DSL scope for configuring a [RegistryBuilder].
 *
 * Provides a declarative API for registering entries,
 * applying reusable contributors, and inspecting configuration state.
 *
 * Nested scopes are restricted by [RegistryDslMarker].
 */
@RegistryDslMarker
class RegistryScope internal constructor(
    private val builder: RegistryBuilder
) {

    /**
     * Configures entries for the specified registry.
     *
     * @param key Typed registry identifier.
     * @param block Entry configuration.
     */
    fun <K : Any, V : Any> registry(
        key: RegistryKey<K, V>,
        block: RegistryEntries<K, V>.() -> Unit
    ) {
        builder.registry(key, block)
    }

    /**
     * Executes a named contributor within this DSL.
     *
     * Registry changes are committed only if the contributor
     * completes successfully.
     *
     * @param id Unique contributor identifier.
     * @param block Nested registry configuration.
     */
    fun contributor(
        id: String,
        block: RegistryScope.() -> Unit
    ) {
        builder.contributor(id) {
            RegistryScope(this).block()
        }
    }

    /**
     * Applies a reusable [RegistryContributor].
     *
     * @param contributor Contributor to apply.
     */
    fun contribute(
        contributor: RegistryContributor
    ) {
        builder.contribute(contributor)
    }

    /**
     * Checks whether a registry with the specified ID exists.
     *
     * @param id Registry identifier.
     * @return True if the registry exists.
     */
    fun containsRegistry(id: RegistryId): Boolean =
        builder.containsRegistry(id)

    /**
     * Checks whether a registry with the specified key's ID exists.
     *
     * Runtime type compatibility is not checked.
     *
     * @param key Typed registry identifier.
     * @return True if the registry exists.
     */
    fun containsRegistry(key: RegistryKey<*, *>): Boolean =
        builder.containsRegistry(key)

    /**
     * Returns the number of entries in the specified registry.
     *
     * @param id Registry identifier.
     * @return Entry count, or null if the registry does not exist.
     */
    fun getEntryCount(id: RegistryId): Int? =
        builder.getEntryCount(id)

    /**
     * Returns the number of entries in the specified registry.
     *
     * @param key Typed registry identifier.
     * @return Entry count, or null if the registry does not exist.
     */
    fun getEntryCount(key: RegistryKey<*, *>): Int? =
        builder.getEntryCount(key)

    /**
     * Number of registries currently configured.
     */
    val registryCount: Int
        get() = builder.registryCount

    /**
     * Number of successfully executed contributors.
     */
    val contributorCount: Int
        get() = builder.contributorCount

    /**
     * IDs of successfully executed contributors in execution order.
     */
    val contributorIds: Set<String>
        get() = builder.contributorIds
}
