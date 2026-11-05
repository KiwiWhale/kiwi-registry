package io.github.kiwiwhale.kiwiregistry.registry

import io.github.kiwiwhale.kiwiregistry.bootstrap.RegistryHolder
import io.github.kiwiwhale.kiwiregistry.core.Registry
import io.github.kiwiwhale.kiwiregistry.core.RegistryKey
import java.util.Collections
import java.util.IdentityHashMap

class FrozenRegistry<K, V>(
    override val registryKey: RegistryKey<K, V>,
    data: Map<K, V>,
    holderData: Map<RegistryHolder, Map<K, V>>
) : Registry<K, V> {

    override val data: Map<K, V> =
        immutableMap(data)

    override val holderData: Map<RegistryHolder, Map<K, V>> =
        immutableHolderData(holderData)

    override val size: Int
        get() = data.size

    override val keys: Set<K>
        get() = data.keys

    override val values: Collection<V>
        get() = data.values

    override val isEmpty: Boolean
        get() = data.isEmpty()

    override val isNotEmpty: Boolean
        get() = data.isNotEmpty()

    override fun containsHolder(holder: RegistryHolder): Boolean =
        holderData.containsKey(holder)

    override fun getHolderData(
        holder: RegistryHolder
    ): Map<K, V> =
        holderData[holder] ?: emptyMap()

    private fun <A, B> immutableMap(
        source: Map<A, B>
    ): Map<A, B> =
        Collections.unmodifiableMap(
            LinkedHashMap(source)
        )

    private fun immutableHolderData(
        source: Map<RegistryHolder, Map<K, V>>
    ): Map<RegistryHolder, Map<K, V>> {

        val snapshot =
            IdentityHashMap<RegistryHolder, Map<K, V>>()

        source.forEach { (holder, entries) ->
            snapshot[holder] = immutableMap(entries)
        }

        return Collections.unmodifiableMap(snapshot)
    }
}
