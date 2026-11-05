package io.github.kiwiwhale.kiwiregistry.core

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

        require(':' !in namespace) {
            "Registry namespace cannot contain ':'."
        }

        require(':' !in key) {
            "Registry key cannot contain ':'."
        }
    }

    companion object {
        fun parse(value: String): RegistryId {
            val separator = value.indexOf(':')

            require(separator > 0 && separator < value.lastIndex) {
                "Invalid registry ID '$value'. Expected format: namespace:key."
            }

            return RegistryId(
                value.substring(0, separator),
                value.substring(separator + 1)
            )
        }
    }

    override fun toString(): String = "$namespace:$key"
}