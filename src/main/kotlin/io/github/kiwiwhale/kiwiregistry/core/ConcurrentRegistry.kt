package io.github.kiwiwhale.kiwiregistry.core

import io.github.kiwiwhale.kiwiregistry.Registry
import io.github.kiwiwhale.kiwiregistry.RegistryKey
import io.github.kiwiwhale.kiwiregistry.exception.DuplicateRegistryEntryException
import io.github.kiwiwhale.kiwiregistry.exception.RegistryEntryNotFoundException
import java.util.Collections
import java.util.concurrent.atomic.AtomicReference

class ConcurrentRegistry<T : Any, K : RegistryKey>(
    private val keyFactory: (String) -> K
) : Registry<T, K> {

    private data class Snapshot<K, T>(
        val byKey: Map<K, T>,
        val byName: Map<String, T>,
        val keyByName: Map<String, K>
    )

    private val state = AtomicReference(
        Snapshot<K, T>(
            byKey = emptyMap(),
            byName = emptyMap(),
            keyByName = emptyMap()
        )
    )

    override val size: Int
        get() = state.get().byKey.size

    override fun register(key: K, value: T) {
        registerAll(linkedMapOf(key to value))
    }

    override fun register(name: String, value: T) {
        validateName(name)

        val key = keyFactory(name)

        require(key.name == name) {
            "Generated key name '${key.name}' does not match '$name'."
        }

        register(key, value)
    }

    override fun registerAll(additions: Map<K, T>) {
        if (additions.isEmpty()) return

        validateEntries(additions)

        while (true) {
            val current = state.get()

            val duplicates = additions.keys
                .map { it.name }
                .filter { it in current.byName }
                .distinct()
                .sorted()

            if (duplicates.isNotEmpty()) {
                throw DuplicateRegistryEntryException(
                    duplicates.joinToString()
                )
            }

            val byKey = LinkedHashMap(current.byKey)
            val byName = LinkedHashMap(current.byName)
            val keyByName = LinkedHashMap(current.keyByName)

            additions.forEach { (key, value) ->
                byKey[key] = value
                byName[key.name] = value
                keyByName[key.name] = key
            }

            val updated = Snapshot(
                byKey = immutableKeyMap(byKey),
                byName = immutableNameMap(byName),
                keyByName = immutableNameMap(keyByName)
            )

            if (state.compareAndSet(current, updated)) {
                return
            }
        }
    }

    override fun get(key: K): T =
        getOrNull(key)
            ?: throw RegistryEntryNotFoundException(key.name)

    override fun get(name: String): T =
        getOrNull(name)
            ?: throw RegistryEntryNotFoundException(name)

    override fun getOrNull(key: K): T? =
        getOrNull(key.name)

    override fun getOrNull(name: String): T? {
        validateName(name)
        return state.get().byName[name]
    }

    override fun contains(key: K): Boolean =
        contains(key.name)

    override fun contains(name: String): Boolean {
        validateName(name)
        return state.get().byName.containsKey(name)
    }

    override fun unregister(key: K): Boolean =
        unregister(key.name)

    override fun unregister(name: String): Boolean {
        validateName(name)

        while (true) {
            val current = state.get()

            val existingKey = current.keyByName[name]
                ?: return false

            val byKey = LinkedHashMap(current.byKey)
            val byName = LinkedHashMap(current.byName)
            val keyByName = LinkedHashMap(current.keyByName)

            byKey.remove(existingKey)
            byName.remove(name)
            keyByName.remove(name)

            val updated = Snapshot(
                byKey = immutableKeyMap(byKey),
                byName = immutableNameMap(byName),
                keyByName = immutableNameMap(keyByName)
            )

            if (state.compareAndSet(current, updated)) {
                return true
            }
        }
    }

    override fun keys(): Set<K> =
        Collections.unmodifiableSet(
            LinkedHashSet(state.get().byKey.keys)
        )

    override fun names(): Set<String> =
        Collections.unmodifiableSet(
            LinkedHashSet(state.get().byName.keys)
        )

    override fun snapshot(): Map<K, T> =
        state.get().byKey

    private fun validateEntries(additions: Map<K, T>) {
        additions.keys.forEach {
            validateName(it.name)
        }

        val duplicates = additions.keys
            .groupBy { it.name }
            .filterValues { it.size > 1 }
            .keys

        if (duplicates.isNotEmpty()) {
            throw DuplicateRegistryEntryException(
                duplicates.sorted().joinToString()
            )
        }
    }

    private fun validateName(name: String) {
        require(name.isNotBlank()) {
            "Registry entry name cannot be blank."
        }
    }

    private fun immutableKeyMap(
        map: Map<K, T>
    ): Map<K, T> =
        Collections.unmodifiableMap(LinkedHashMap(map))

    private fun <V> immutableNameMap(
        map: Map<String, V>
    ): Map<String, V> =
        Collections.unmodifiableMap(LinkedHashMap(map))
}
