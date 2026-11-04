package io.github.kiwiwhale.kiwiregistry.bootstrap

import io.github.kiwiwhale.kiwiregistry.RegistryKey
import io.github.kiwiwhale.kiwiregistry.core.FrozenRegistry

interface BootstrapHolder {

    val bootstrap: Bootstrap

    operator fun <T : Any> get(
        key: RegistryKey<T>
    ): FrozenRegistry<T> =
        bootstrap[key]

    fun <T : Any> getOrNull(
        key: RegistryKey<T>
    ): FrozenRegistry<T>? =
        bootstrap.getOrNull(key)
}
