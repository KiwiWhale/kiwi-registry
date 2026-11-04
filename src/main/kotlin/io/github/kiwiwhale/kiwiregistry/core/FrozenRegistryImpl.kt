package io.github.kiwiwhale.kiwiregistry.core

import io.github.kiwiwhale.kiwiregistry.RegistryId
import io.github.kiwiwhale.kiwiregistry.exception.RegistryEntryNotFoundException

internal class FrozenRegistryImpl<T : Any>(
    override val id: RegistryId,
    private val snapshot: RegistrySnapshot<T>
) : FrozenRegistry<T> {

    override val size: Int
        get() = snapshot.size

    override fun get(key: String): T =
        getOrNull(key)
            ?: throw RegistryEntryNotFoundException(id, key)

    override fun getOrNull(key: String): T? {
        validateKey(key)
        return snapshot[key]
    }

    override fun contains(key: String): Boolean {
        validateKey(key)
        return key in snapshot
    }

    override fun keys(): Set<String> =
        snapshot.keys()

    override fun values(): Collection<T> =
        snapshot.values()

    override fun entries(): Set<Map.Entry<String, T>> =
        snapshot.entries()

    override fun snapshot(): Map<String, T> =
        snapshot.asMap()

    override fun snapshotData(): RegistrySnapshot<T> =
        snapshot

    override fun thaw(): MutableRegistry<T> =
        MutableRegistryImpl(
            id = id,
            initial = snapshot.asMap()
        )

    private fun validateKey(key: String) {
        require(key.isNotBlank()) {
            "Registry key cannot be blank."
        }

        require(':' !in key) {
            "Registry key cannot contain ':'."
        }
    }
}
