package io.github.kiwiwhale.kiwiregistry

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
        fun of(
            namespace: String,
            key: String
        ): RegistryId =
            RegistryId(namespace, key)

        fun parse(value: String): RegistryId {
            val separator = value.indexOf(':')

            require(separator > 0) {
                "Invalid registry ID: '$value'."
            }

            require(separator == value.lastIndexOf(':')) {
                "Invalid registry ID: '$value'."
            }

            require(separator < value.lastIndex) {
                "Invalid registry ID: '$value'."
            }

            return RegistryId(
                value.substring(0, separator),
                value.substring(separator + 1)
            )
        }
    }

    override fun toString(): String = "$namespace:$key"
}
