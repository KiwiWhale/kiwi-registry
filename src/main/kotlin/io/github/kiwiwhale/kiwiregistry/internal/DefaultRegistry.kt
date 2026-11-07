package io.github.kiwiwhale.kiwiregistry.internal

import io.github.kiwiwhale.kiwiregistry.core.Registry
import io.github.kiwiwhale.kiwiregistry.core.RegistryKey
import java.util.*

/**
 * Default immutable registry implementation.
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
