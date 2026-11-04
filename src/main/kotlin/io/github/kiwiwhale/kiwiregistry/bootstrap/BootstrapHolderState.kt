package io.github.kiwiwhale.kiwiregistry.bootstrap

import io.github.kiwiwhale.kiwiregistry.exception.BootstrapAlreadyInjectedException
import io.github.kiwiwhale.kiwiregistry.exception.BootstrapNotInjectedException
import java.util.IdentityHashMap

internal object BootstrapHolderState {

    private val bootstraps = IdentityHashMap<BootstrapHolder, RegistryBootstrap>()

    @Synchronized
    fun set(
        holder: BootstrapHolder,
        bootstrap: RegistryBootstrap
    ) {
        if (holder in bootstraps) {
            throw BootstrapAlreadyInjectedException()
        }

        bootstraps[holder] = bootstrap
    }

    @Synchronized
    fun get(holder: BootstrapHolder): RegistryBootstrap {
        return bootstraps[holder]
            ?: throw BootstrapNotInjectedException()
    }

    @Synchronized
    fun remove(holder: BootstrapHolder) {
        bootstraps.remove(holder)
    }
}
