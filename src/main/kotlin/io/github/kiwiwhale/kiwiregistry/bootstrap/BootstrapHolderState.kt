package io.github.kiwiwhale.kiwiregistry.bootstrap

import java.util.IdentityHashMap

internal object BootstrapHolderState {

    private val bootstraps = IdentityHashMap<BootstrapHolder, RegistryBootstrap>()

    @Synchronized
    fun set(
        holder: BootstrapHolder,
        bootstrap: RegistryBootstrap
    ) {
        check(holder !in bootstraps) {
            "BootstrapHolder already injected"
        }

        bootstraps[holder] = bootstrap
    }

    @Synchronized
    fun get(holder: BootstrapHolder): RegistryBootstrap {
        return bootstraps[holder]
            ?: error("BootstrapHolder has not been injected")
    }
}
