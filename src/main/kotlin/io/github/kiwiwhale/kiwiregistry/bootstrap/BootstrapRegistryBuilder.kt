package io.github.kiwiwhale.kiwiregistry.bootstrap

import io.github.kiwiwhale.kiwiregistry.RegistryId
import io.github.kiwiwhale.kiwiregistry.core.FrozenRegistry
import java.util.Collections
import kotlin.reflect.KClass

class BootstrapRegistryBuilder<T : Any> internal constructor(
    val id: RegistryId,
    internal val type: KClass<T>
) {
    private val entries = LinkedHashMap<String, T>()

    fun add(
        key: String,
        value: T
    ) {
        validateKey(key)

        check(!entries.containsKey(key)) {
            "Duplicate registry entry: '$id:$key'."
        }

        entries[key] = value
    }

    fun addAll(
        values: Map<String, T>
    ) {
        val additions = LinkedHashMap(values)

        additions.keys.forEach(::validateKey)

        val duplicates = additions.keys.filter {
            it in entries
        }

        check(duplicates.isEmpty()) {
            "Duplicate registry entries: ${duplicates.joinToString()}."
        }

        entries.putAll(additions)
    }

    val size: Int
        get() = entries.size

    fun contains(key: String): Boolean =
        key in entries

    fun keys(): Set<String> =
        Collections.unmodifiableSet(
            LinkedHashSet(entries.keys)
        )

    internal fun freeze(): FrozenRegistry<T> =
        FrozenRegistry.of(
            id = id,
            entries = entries
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
