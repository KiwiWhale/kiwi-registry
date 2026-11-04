package io.github.kiwiwhale.kiwiregistry.bootstrap

import io.github.kiwiwhale.kiwiregistry.core.Registry
import io.github.kiwiwhale.kiwiregistry.core.RegistryKey

interface BootstrapHolder {

    fun inject(): () -> Unit {
        val bootstrap = RegistryBootstrap()

        BootstrapHolderState.set(this, bootstrap)

        return {
            bootstrap.build()
        }
    }

    fun <K, V> registry(
        key: RegistryKey<K, V>
    ): Registry<K, V> {
        return BootstrapHolderState
            .get(this)
            .getRegistry(key)
    }

    operator fun <K, V> get(
        key: RegistryKey<K, V>
    ): Registry<K, V> = registry(key)

    fun rebuildRegistries(holder: RegistryHolder) {
        BootstrapHolderState
            .get(this)
            .rebuildRegistries(holder)
    }
}
