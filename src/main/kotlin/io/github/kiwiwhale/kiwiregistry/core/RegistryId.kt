package io.github.kiwiwhale.kiwiregistry.core

/**
 * Unique registry identifier.
 *
 * Registry IDs consist of a namespace and a key.
 *
 * Format: `namespace:key`
 *
 * Both components must be non-blank and cannot contain
 * surrounding whitespace or the ':' separator.
 *
 * @property namespace Namespace identifying the registry owner.
 * @property key Name identifying the registry within its namespace.
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

        require(namespace == namespace.trim()) {
            "Registry namespace cannot contain surrounding whitespace."
        }

        require(key == key.trim()) {
            "Registry key cannot contain surrounding whitespace."
        }

        require(':' !in namespace && ':' !in key) {
            "Registry ID components cannot contain ':'."
        }
    }

    /**
     * Returns this identifier in `namespace:key` format.
     */
    override fun toString(): String =
        "$namespace:$key"

    companion object {

        /**
         * Parses a registry ID from its string representation.
         *
         * The input must contain exactly one ':' separator,
         * with non-empty components on both sides.
         *
         * @param value String representation in `namespace:key` format.
         * @return Parsed registry identifier.
         *
         * @throws IllegalArgumentException If the input does not
         * follow the required format or contains invalid components.
         */
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
