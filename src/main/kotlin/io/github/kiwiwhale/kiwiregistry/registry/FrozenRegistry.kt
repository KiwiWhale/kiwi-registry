package io.github.kiwiwhale.kiwiregistry.registry

import io.github.kiwiwhale.kiwiregistry.bootstrap.RegistryHolder
import io.github.kiwiwhale.kiwiregistry.core.Registry
import io.github.kiwiwhale.kiwiregistry.core.RegistryKey

class FrozenRegistry<K, V>(
    override val registryKey: RegistryKey<K, V>,
    data: Map<K, V>,
    holderData: Map<RegistryHolder, Map<K, V>>
) : Registry<K, V> {

    override val data: Map<K, V> = data.toMap()

    override val holderData: Map<RegistryHolder, Map<K, V>> =
        holderData.mapValues { (_, entries) ->
            entries.toMap()
        }.toMap()

    override fun withData(
        data: Map<K, V>,
        holderData: Map<RegistryHolder, Map<K, V>>
    ): FrozenRegistry<K, V> {
        return FrozenRegistry(
            registryKey,
            data,
            holderData
        )
    }
}
