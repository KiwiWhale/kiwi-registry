package io.github.kiwiwhale.kiwiregistry

/**
 * Unique identifier for a registry entry.
 *
 * Format: namespace:key
 *
 * Both namespace and key must be non-blank and must not contain ':'.
 */
data class RegistryId(
    val namespace: String,
    val key: String
) {

    init {
        require(namespace.isNotBlank()) {
            "Registry namespace must not be blank."
        }

        require(':' !in namespace) {
            "Registry namespace must not contain ':'."
        }

        require(key.isNotBlank()) {
            "Registry key must not be blank."
        }

        require(':' !in key) {
            "Registry key must not contain ':'."
        }
    }

    override fun toString(): String =
        "$namespace:$key"

    companion object {

        /**
         * Parses a registry ID.
         *
         * @throws IllegalArgumentException If the format is invalid.
         */
        @JvmStatic
        fun parse(value: String): RegistryId {
            val separator = value.indexOf(':')

            require(
                separator > 0 &&
                        separator < value.lastIndex &&
                        value.indexOf(':', separator + 1) == -1
            ) {
                "Invalid registry ID: '$value'."
            }

            return RegistryId(
                namespace = value.substring(0, separator),
                key = value.substring(separator + 1)
            )
        }
    }
}
