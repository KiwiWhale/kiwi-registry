package io.github.kiwiwhale.kiwiregistry.dsl

import io.github.kiwiwhale.kiwiregistry.builder.RegistryBuilder
import io.github.kiwiwhale.kiwiregistry.core.RegistryGraph

/**
 * Creates an immutable RegistryGraph using the Kotlin DSL.
 */
fun registryGraph(
    block: RegistryScope.() -> Unit
): RegistryGraph {
    val builder = RegistryBuilder()

    RegistryScope(builder).block()

    return builder.build()
}
