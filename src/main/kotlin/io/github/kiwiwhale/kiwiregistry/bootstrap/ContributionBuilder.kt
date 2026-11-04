package io.github.kiwiwhale.kiwiregistry.bootstrap

import io.github.kiwiwhale.kiwiregistry.core.RegistryKey
import io.github.kiwiwhale.kiwiregistry.exception.InvalidContributionException

class ContributionBuilder {

    private val registries = LinkedHashMap<RegistryKey<*, *>, RegistryContributionBuilder<*, *>.() -> Unit>()
    private val preHooks = ArrayList<() -> Unit>()
    private val postHooks = ArrayList<() -> Unit>()

    @Suppress("UNCHECKED_CAST")
    fun <K, V> registry(
        key: RegistryKey<K, V>,
        builder: RegistryContributionBuilder<K, V>.() -> Unit
    ) {
        if (key in registries) {
            throw InvalidContributionException("Registry already declared in this contribution: ${key.id}")
        }

        registries[key] = builder as RegistryContributionBuilder<*, *>.() -> Unit
    }

    fun pre(block: () -> Unit) {
        preHooks += block
    }

    fun post(block: () -> Unit) {
        postHooks += block
    }

    internal fun build(holder: RegistryHolder): Contribution {
        return Contribution(
            holder = holder,
            registries = registries.toMap(),
            preHooks = preHooks.toList(),
            postHooks = postHooks.toList()
        )
    }
}
