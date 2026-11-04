package io.github.kiwiwhale.kiwiregistry.registry

import io.github.kiwiwhale.kiwiregistry.bootstrap.RegistryHolder
import io.github.kiwiwhale.kiwiregistry.core.Registry
import io.github.kiwiwhale.kiwiregistry.core.RegistryKey
import io.github.kiwiwhale.kiwiregistry.exception.DuplicateRegistryEntryException

class MutableRegistry<K, V>(
    override val registryKey: RegistryKey<K, V>
) : Registry<K, V> {

    private val mutableData = LinkedHashMap<K, V>()

    private val mutableHolderData =
        LinkedHashMap<RegistryHolder, LinkedHashMap<K, V>>()

    override val data: Map<K, V>
        get() = mutableData.toMap()

    override val holderData: Map<RegistryHolder, Map<K, V>>
        get() = mutableHolderData.mapValues { (_, entries) ->
            entries.toMap()
        }

    fun register(
        holder: RegistryHolder,
        key: K,
        value: V
    ) {
        if (key in mutableData) {
            throw DuplicateRegistryEntryException(
                registryId = registryKey.id.toString(),
                key = key
            )
        }

        mutableData[key] = value

        mutableHolderData
            .getOrPut(holder) { LinkedHashMap() }[key] = value
    }

    fun unregister(
        holder: RegistryHolder,
        key: K
    ): V? {
        val holderEntries = mutableHolderData[holder]
            ?: return null

        val value = holderEntries.remove(key)
            ?: return null

        mutableData.remove(key)

        if (holderEntries.isEmpty()) {
            mutableHolderData.remove(holder)
        }

        return value
    }

    fun unregisterHolder(holder: RegistryHolder) {
        val entries = mutableHolderData.remove(holder)
            ?: return

        entries.keys.forEach(mutableData::remove)
    }

    fun clear() {
        mutableData.clear()
        mutableHolderData.clear()
    }

    fun freeze(): FrozenRegistry<K, V> {
        return FrozenRegistry(
            registryKey = registryKey,
            data = mutableData,
            holderData = mutableHolderData
        )
    }

    override fun withData(
        data: Map<K, V>,
        holderData: Map<RegistryHolder, Map<K, V>>
    ): FrozenRegistry<K, V> {
        return FrozenRegistry(
            registryKey = registryKey,
            data = data,
            holderData = holderData
        )
    }
}
