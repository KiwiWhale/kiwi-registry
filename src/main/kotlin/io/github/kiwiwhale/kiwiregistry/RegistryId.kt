package io.github.kiwiwhale.kiwiregistry

/**
 * Unique identifier for a registry entry.
 *
 * Consists of a namespace and a key.
 *
 * Example: `kiwi-flow:workflow`
 *
 * @property namespace Identifier namespace.
 * @property key Entry key.
 */
data class RegistryId(
    val namespace: String,
    val key: String
) {

    init {
        require(namespace.isNotBlank() && ':' !in namespace)
        require(key.isNotBlank() && ':' !in key)
    }

    override fun toString(): String = "$namespace:$key"

    companion object {

        /**
         * Parses an identifier from `namespace:key` format.
         *
         * @throws IllegalArgumentException If the format is invalid.
         */
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