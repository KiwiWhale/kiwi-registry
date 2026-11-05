package io.github.kiwiwhale.kiwiregistry.registry

import io.github.kiwiwhale.kiwiregistry.bootstrap.RegistryHolder
import io.github.kiwiwhale.kiwiregistry.core.Registry
import io.github.kiwiwhale.kiwiregistry.core.RegistryKey
import io.github.kiwiwhale.kiwiregistry.exception.DuplicateRegistryEntryException
import java.util.Collections
import java.util.IdentityHashMap

class MutableRegistry<K, V>(
    override val registryKey: RegistryKey<K, V>
) : Registry<K, V> {

    private val mutableData =
        LinkedHashMap<K, V>()

    private val mutableHolderData =
        IdentityHashMap<RegistryHolder, LinkedHashMap<K, V>>()

    override val size: Int
        get() = mutableData.size

    override val isEmpty: Boolean
        get() = mutableData.isEmpty()

    override val isNotEmpty: Boolean
        get() = mutableData.isNotEmpty()

    override val data: Map<K, V>
        get() = immutableMap(mutableData)

    override val holderData: Map<RegistryHolder, Map<K, V>>
        get() = immutableHolderData()

    override val keys: Set<K>
        get() = Collections.unmodifiableSet(
            LinkedHashSet(mutableData.keys)
        )

    override val values: Collection<V>
        get() = Collections.unmodifiableList(
            ArrayList(mutableData.values)
        )

    fun register(
        holder: RegistryHolder,
        key: K,
        value: V
    ) {
        if (mutableData.containsKey(key)) {
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

        val holderEntries =
            mutableHolderData[holder] ?: return null

        if (!holderEntries.containsKey(key)) {
            return null
        }

        val value = holderEntries.remove(key)

        mutableData.remove(key)

        if (holderEntries.isEmpty()) {
            mutableHolderData.remove(holder)
        }

        return value
    }

    fun unregisterHolder(holder: RegistryHolder) {
        val entries =
            mutableHolderData.remove(holder) ?: return

        entries.keys.forEach { key ->
            mutableData.remove(key)
        }
    }

    fun clear() {
        mutableData.clear()
        mutableHolderData.clear()
    }

    fun freeze(): FrozenRegistry<K, V> =
        FrozenRegistry(
            registryKey = registryKey,
            data = mutableData,
            holderData = immutableHolderData()
        )

    private fun immutableHolderData():
            Map<RegistryHolder, Map<K, V>> {

        val snapshot =
            IdentityHashMap<RegistryHolder, Map<K, V>>()

        mutableHolderData.forEach { (holder, entries) ->
            snapshot[holder] = immutableMap(entries)
        }

        return Collections.unmodifiableMap(snapshot)
    }

    private fun <A, B> immutableMap(
        source: Map<A, B>
    ): Map<A, B> =
        Collections.unmodifiableMap(
            LinkedHashMap(source)
        )
}
