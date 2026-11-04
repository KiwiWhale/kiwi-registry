package io.github.kiwiwhale.kiwiregistry.core

import io.github.kiwiwhale.kiwiregistry.Registry
import io.github.kiwiwhale.kiwiregistry.RegistryId

interface FrozenRegistry<T : Any> : Registry<T> {

    fun thaw(): MutableRegistry<T>

    fun snapshotData(): RegistrySnapshot<T>

    companion object {
        fun <T : Any> of(
            id: RegistryId,
            entries: Map<String, T>
        ): FrozenRegistry<T> =
            FrozenRegistryImpl(
                id,
                RegistrySnapshot(entries)
            )
    }
}
