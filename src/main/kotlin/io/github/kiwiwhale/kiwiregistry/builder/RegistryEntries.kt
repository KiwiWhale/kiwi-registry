package io.github.kiwiwhale.kiwiregistry.builder

import io.github.kiwiwhale.kiwiregistry.core.RegistryKey
import io.github.kiwiwhale.kiwiregistry.exception.RegistryBuildException

/**
 * Mutable entry configuration scope.
 *
 * This object is only valid during its registry configuration block.
 */
class RegistryEntries<K : Any, V : Any> internal constructor(
    private val key: RegistryKey<K, V>,
    private val entries: MutableMap<Any, Any>,
    private val contributor: String
) {

    private var active = true

    internal fun close() {
        active = false
    }

    private fun checkActive() {
        check(active) {
            "RegistryEntries is no longer active."
        }
    }

    /**
     * Adds an entry.
     *
     * Duplicate keys are not allowed.
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

    val size: Int
        get() {
            checkActive()
            return entries.size
        }

    val isEmpty: Boolean
        get() {
            checkActive()
            return entries.isEmpty()
        }

    val isNotEmpty: Boolean
        get() = !isEmpty
}
