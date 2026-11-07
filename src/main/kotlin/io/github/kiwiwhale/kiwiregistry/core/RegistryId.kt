package io.github.kiwiwhale.kiwiregistry.core

/**
 * Unique registry identifier.
 *
 * Format: namespace:key
 */
data class RegistryId(
    val namespace: String,
    val key: String
) {

    init {
        require(namespace.isNotBlank()) {
            "Registry namespace cannot be blank."
        }

        require(key.isNotBlank()) {
            "Registry key cannot be blank."
        }

        require(':' !in namespace && ':' !in key) {
            "Registry ID components cannot contain ':'."
        }
    }

    override fun toString(): String =
        "$namespace:$key"

    companion object {
        fun parse(value: String): RegistryId {
            val separator = value.indexOf(':')

            require(separator > 0 && separator < value.lastIndex) {
                "Invalid registry ID '$value'. Expected format: namespace:key."
            }

            require(value.indexOf(':', separator + 1) == -1) {
                "Invalid registry ID '$value'."
            }

            return RegistryId(
                value.substring(0, separator),
                value.substring(separator + 1)
            )
        }
    }

}