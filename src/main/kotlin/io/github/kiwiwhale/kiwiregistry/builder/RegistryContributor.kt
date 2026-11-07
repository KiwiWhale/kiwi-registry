package io.github.kiwiwhale.kiwiregistry.builder

/**
 * Reusable registry configuration contributor.
 *
 * Each contributor has a unique ID within a [RegistryBuilder].
 *
 * A contributor can be applied to multiple builders, but it can only
 * be applied once to each builder.
 *
 * Configuration is executed transactionally by the target builder.
 *
 * @param id Unique contributor identifier.
 * @param configuration Registry configuration to execute.
 */
class RegistryContributor(
    val id: String,
    private val configuration: RegistryBuilder.() -> Unit
) {

    init {
        require(id.isNotBlank()) {
            "Contributor ID cannot be blank."
        }

        require(id == id.trim()) {
            "Contributor ID cannot contain surrounding whitespace."
        }
    }

    /**
     * Applies this contributor to the specified builder.
     *
     * @param builder Target builder.
     */
    internal fun applyTo(builder: RegistryBuilder) {
        builder.contributor(id, configuration)
    }

    override fun toString(): String =
        "RegistryContributor(id='$id')"
}
