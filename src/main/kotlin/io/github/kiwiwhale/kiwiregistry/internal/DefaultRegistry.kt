package io.github.kiwiwhale.kiwiregistry.internal

import io.github.kiwiwhale.kiwiregistry.core.Registry
import io.github.kiwiwhale.kiwiregistry.core.RegistryKey
import java.util.*

/**
 * Default immutable registry implementation.
 *
 * Copies the supplied entries into an unmodifiable LinkedHashMap,
 * preserving insertion order.
 *
 * Keys and values are not deep-copied.
 *
 * @param K Registry key type.
 * @param V Registry value type.
 * @param key Typed registry identifier.
 * @param source Source entries to snapshot.
 */
internal class DefaultRegistry<K : Any, V : Any>(
    override val key: RegistryKey<K, V>,
    source: Map<K, V>
) : Registry<K, V> {

    override val data: Map<K, V> =
        Collections.unmodifiableMap(
            LinkedHashMap(source)
        )
}
