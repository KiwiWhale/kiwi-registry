package io.github.kiwiwhale.kiwiregistry.dsl

import io.github.kiwiwhale.kiwiregistry.builder.RegistryBuilder
import io.github.kiwiwhale.kiwiregistry.builder.RegistryContributor
import io.github.kiwiwhale.kiwiregistry.builder.RegistryEntries
import io.github.kiwiwhale.kiwiregistry.core.RegistryKey

@RegistryDslMarker
class RegistryScope internal constructor(
    private val builder: RegistryBuilder
) {

    fun <K : Any, V : Any> registry(
        key: RegistryKey<K, V>,
        block: RegistryEntries<K, V>.() -> Unit
    ) {
        builder.registry(key, block)
    }

    fun contributor(
        id: String,
        block: RegistryScope.() -> Unit
    ) {
        builder.contributor(id) {
            RegistryScope(this).block()
        }
    }

    fun contribute(
        contributor: RegistryContributor
    ) {
        builder.contribute(contributor)
    }
}
