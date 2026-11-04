package io.github.kiwiwhale.kiwiregistry.bootstrap

import io.github.kiwiwhale.kiwiregistry.RegistryId
import io.github.kiwiwhale.kiwiregistry.RegistryKey
import io.github.kiwiwhale.kiwiregistry.exception.DuplicateRegistryException

class BootstrapBuilder internal constructor() {

    internal val registries =
        LinkedHashMap<RegistryId, BootstrapRegistryBuilder<*>>()

    private var built = false

    fun <T : Any> registry(
        key: RegistryKey<T>,
        configure: BootstrapRegistryBuilder<T>.() -> Unit
    ) {
        checkNotBuilt()

        if (key.id in registries) {
            throw DuplicateRegistryException(key.id)
        }

        val builder = BootstrapRegistryBuilder(
            id = key.id,
            type = key.type
        )

        builder.configure()

        registries[key.id] = builder
    }

    internal fun markBuilt() {
        checkNotBuilt()
        built = true
    }

    private fun checkNotBuilt() {
        check(!built) {
            "BootstrapBuilder has already been built."
        }
    }
}
