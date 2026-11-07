package io.github.kiwiwhale.kiwiregistry.dsl

import io.github.kiwiwhale.kiwiregistry.builder.RegistryBuilder
import io.github.kiwiwhale.kiwiregistry.builder.RegistryContributor
import io.github.kiwiwhale.kiwiregistry.builder.RegistryEntries
import io.github.kiwiwhale.kiwiregistry.core.RegistryKey

/**
 * DSL scope for configuring a [RegistryBuilder].
 *
 * Provides a declarative API for registering entries and
 * applying reusable contributors.
 *
 * Nested scopes are restricted by [RegistryDslMarker].
 *
 * @property builder Builder receiving the configuration.
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
}
