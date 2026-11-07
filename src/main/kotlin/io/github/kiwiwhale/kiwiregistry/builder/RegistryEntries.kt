package io.github.kiwiwhale.kiwiregistry.builder

import io.github.kiwiwhale.kiwiregistry.core.RegistryKey
import io.github.kiwiwhale.kiwiregistry.exception.RegistryBuildException

/**
 * Mutable entry configuration scope.
 *
 * This object is only valid during its registry configuration block.
 *
 * Entries are committed by [RegistryBuilder] only after successful
 * completion of the configuration block.
 *
 * This class is not thread-safe.
 *
 * @param K Registry key type.
 * @param V Registry value type.
 */
class RegistryEntries<K : Any, V : Any> internal constructor(
    private val key: RegistryKey<K, V>,
    private val entries: MutableMap<Any, Any>,
    private val contributor: String
) {

    private var active = true

    /**
     * Closes this configuration scope.
     *
     * Further operations on this instance are rejected.
     */
    internal fun close() {
        active = false
    }

    private fun checkActive() {
        check(active) {
            "RegistryEntries is no longer active."
        }
    }

    /**
     * Adds an entry to the registry.
     *
     * Both key and value are checked against the registry's
     * runtime types before insertion.
     *
     * Duplicate keys are not allowed.
     *
     * @param key Entry key.
     * @param value Entry value.
     *
     * @throws IllegalStateException If this configuration scope
     * is no longer active.
     * @throws IllegalArgumentException If the key or value has
     * an incompatible runtime type.
     * @throws RegistryBuildException If the key already exists.
     */
    fun add(key: K, value: V) {
        checkActive()

        require(this.key.key.isInstance(key)) {
            "Invalid key type in registry '${this.key.id}'."
        }

        require(this.key.value.isInstance(value)) {
            "Invalid value type in registry '${this.key.id}'."
        }

        if (entries.containsKey(key)) {
            throw RegistryBuildException(
                "Duplicate entry '$key' in registry '${this.key.id}' " +
                        "from contributor '$contributor'."
            )
        }

        entries[key] = value
    }

    /**
     * Number of entries currently configured.
     *
     * @throws IllegalStateException If this configuration scope
     * is no longer active.
     */
    val size: Int
        get() {
            checkActive()
            return entries.size
        }

    /**
     * Whether this registry currently contains no entries.
     *
     * @throws IllegalStateException If this configuration scope
     * is no longer active.
     */
    val isEmpty: Boolean
        get() {
            checkActive()
            return entries.isEmpty()
        }

    /**
     * Whether this registry currently contains at least one entry.
     *
     * @throws IllegalStateException If this configuration scope
     * is no longer active.
     */
    val isNotEmpty: Boolean
        get() = !isEmpty
}
