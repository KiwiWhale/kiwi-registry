package io.github.kiwiwhale.kiwiregistry.builder

/**
 * Reusable registry configuration contributor.
 *
 * Each contributor has a unique ID within a RegistryBuilder.
 */
class RegistryContributor(
    val id: String,
    private val configuration: RegistryBuilder.() -> Unit
) {

    init {
        require(id.isNotBlank()) {
            "Contributor ID cannot be blank."
        }
    }

    internal fun applyTo(builder: RegistryBuilder) {
        builder.contributor(id, configuration)
    }

    override fun toString(): String =
        "RegistryContributor(id='$id')"
}
