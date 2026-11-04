package io.github.kiwiwhale.kiwiregistry.core

import io.github.kiwiwhale.kiwiregistry.Registry
import io.github.kiwiwhale.kiwiregistry.RegistryId
import io.github.kiwiwhale.kiwiregistry.RegistryKey

interface MutableRegistry<T : Any> : Registry<T> {

    fun register(key: String, value: T)

    fun register(key: RegistryKey<T>, value: T) {
        require(key.id.namespace == id.namespace) {
            "Registry namespace mismatch."
        }

        register(key.id.key, value)
    }

    fun registerAll(entries: Map<String, T>)

    fun registerAllByKey(
        entries: Map<RegistryKey<T>, T>
    ) {
        entries.keys.forEach { key ->
            require(key.id.namespace == id.namespace) {
                "Registry namespace mismatch."
            }
        }

        registerAll(
            entries.mapKeys { it.key.id.key }
        )
    }

    fun registerIfAbsent(key: String, value: T): Boolean

    fun replace(key: String, value: T): Boolean

    fun unregister(key: String): Boolean

    fun clear()

    fun freeze(): FrozenRegistry<T>

    fun snapshotData(): RegistrySnapshot<T>

    companion object {
        fun <T : Any> create(
            id: RegistryId
        ): MutableRegistry<T> =
            MutableRegistryImpl(id)

        fun <T : Any> create(
            key: RegistryKey<T>
        ): MutableRegistry<T> =
            create(key.id)
    }
}
