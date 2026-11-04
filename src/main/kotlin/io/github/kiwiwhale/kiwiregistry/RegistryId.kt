package io.github.kiwiwhale.kiwiregistry

data class RegistryId(
    val namespace: String,
    val key: String
) {

    override fun toString(): String = "$namespace:$key"

    companion object {
        fun of(
            namespace: String,
            key: String
        ): RegistryId =
            RegistryId(namespace, key)

        fun parse(value: String): RegistryId {
            val separator = value.indexOf(':')

            return RegistryId(
                namespace = value.substring(0, separator),
                key = value.substring(separator + 1)
            )
        }
    }
}
