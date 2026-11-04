package io.github.kiwiwhale.kiwiregistry.bootstrap

import io.github.kiwiwhale.kiwiregistry.core.RegistryKey

internal class Contribution(
    val holder: RegistryHolder,
    val registries: Map<RegistryKey<*, *>, RegistryContributionBuilder<*, *>.() -> Unit>,
    val preHooks: List<() -> Unit>,
    val postHooks: List<() -> Unit>
)
