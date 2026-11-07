package io.github.kiwiwhale.kiwiregistry.dsl

import io.github.kiwiwhale.kiwiregistry.builder.RegistryBuilder
import io.github.kiwiwhale.kiwiregistry.core.RegistryGraph

/**
 * Creates an immutable [RegistryGraph] using the Kotlin DSL.
 *
 * A new [RegistryBuilder] is created for each invocation.
 * The resulting graph is returned only after successful configuration
 * and construction.
 *
 * @param block Registry configuration.
 * @return Constructed immutable registry graph.
 *
 * @throws Exception If configuration or graph construction fails.
 */
fun registryGraph(
    block: RegistryScope.() -> Unit
): RegistryGraph {
    val builder = RegistryBuilder()

    RegistryScope(builder).block()

    return builder.build()
}
