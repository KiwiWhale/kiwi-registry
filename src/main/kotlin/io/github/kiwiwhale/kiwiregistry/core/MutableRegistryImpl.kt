package io.github.kiwiwhale.kiwiregistry.core

import io.github.kiwiwhale.kiwiregistry.RegistryId
import io.github.kiwiwhale.kiwiregistry.exception.DuplicateEntryException
import io.github.kiwiwhale.kiwiregistry.exception.RegistryEntryNotFoundException

internal class MutableRegistryImpl<T : Any>(
    override val id: RegistryId,
    initial: Map<String, T> = emptyMap()
) : MutableRegistry<T> {

    private val state = RegistryState(initial)

    init {
        initial.keys.forEach(::validateKey)
    }

    override val size: Int
        get() = state.get().size

    override fun get(key: String): T =
        getOrNull(key)
            ?: throw RegistryEntryNotFoundException(id, key)

    override fun getOrNull(key: String): T? {
        validateKey(key)
        return state.get()[key]
    }

    override fun contains(key: String): Boolean {
        validateKey(key)
        return key in state.get()
    }

    override fun keys(): Set<String> =
        state.get().keys()

    override fun values(): Collection<T> =
        state.get().values()

    override fun entries(): Set<Map.Entry<String, T>> =
        state.get().entries()

    override fun snapshot(): Map<String, T> =
        state.get().asMap()

    override fun snapshotData(): RegistrySnapshot<T> =
        state.get()

    override fun register(
        key: String,
        value: T
    ) {
        validateKey(key)

        state.update { current ->
            if (key in current) {
                throw DuplicateEntryException(
                    id.toString(),
                    key
                )
            }

            val updated = current.mutableCopy()
            updated[key] = value

            RegistrySnapshot(updated)
        }
    }

    override fun registerAll(
        entries: Map<String, T>
    ) {
        if (entries.isEmpty()) return

        val additions = LinkedHashMap(entries)

        additions.keys.forEach(::validateKey)

        state.update { current ->
            val duplicates = additions.keys.filter {
                it in current
            }

            if (duplicates.isNotEmpty()) {
                throw DuplicateEntryException(
                    id.toString(),
                    duplicates
                )
            }

            val updated = current.mutableCopy()
            updated.putAll(additions)

            RegistrySnapshot(updated)
        }
    }

    override fun registerIfAbsent(
        key: String,
        value: T
    ): Boolean {
        validateKey(key)

        while (true) {
            val current = state.get()

            if (key in current) {
                return false
            }

            val updated = current.mutableCopy()
            updated[key] = value

            if (
                state.compareAndSet(
                    current,
                    RegistrySnapshot(updated)
                )
            ) {
                return true
            }
        }
    }

    override fun replace(
        key: String,
        value: T
    ): Boolean {
        validateKey(key)

        while (true) {
            val current = state.get()

            if (key !in current) {
                return false
            }

            val updated = current.mutableCopy()
            updated[key] = value

            if (
                state.compareAndSet(
                    current,
                    RegistrySnapshot(updated)
                )
            ) {
                return true
            }
        }
    }

    override fun unregister(key: String): Boolean {
        validateKey(key)

        while (true) {
            val current = state.get()

            if (key !in current) {
                return false
            }

            val updated = current.mutableCopy()
            updated.remove(key)

            if (
                state.compareAndSet(
                    current,
                    RegistrySnapshot(updated)
                )
            ) {
                return true
            }
        }
    }

    override fun clear() {
        state.update {
            RegistrySnapshot(emptyMap())
        }
    }

    override fun freeze(): FrozenRegistry<T> =
        FrozenRegistryImpl(
            id,
            state.get()
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
