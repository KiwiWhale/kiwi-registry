package io.github.kiwiwhale.kiwiregistry.core

import java.util.Collections
import java.util.LinkedHashMap

class RegistrySnapshot<T : Any> internal constructor(
    entries: Map<String, T>
) {
    private val data: Map<String, T> =
        Collections.unmodifiableMap(
            LinkedHashMap(entries)
        )

    val size: Int
        get() = data.size

    val isEmpty: Boolean
        get() = data.isEmpty()

    operator fun get(key: String): T? =
        data[key]

    operator fun contains(key: String): Boolean =
        key in data

    fun keys(): Set<String> =
        data.keys

    fun values(): Collection<T> =
        data.values

    fun entries(): Set<Map.Entry<String, T>> =
        data.entries

    fun asMap(): Map<String, T> =
        data

    internal fun mutableCopy(): LinkedHashMap<String, T> =
        LinkedHashMap(data)
}
