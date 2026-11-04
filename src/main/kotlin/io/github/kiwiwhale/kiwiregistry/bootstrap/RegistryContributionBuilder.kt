package io.github.kiwiwhale.kiwiregistry.bootstrap

import io.github.kiwiwhale.kiwiregistry.core.RegistryKey
import io.github.kiwiwhale.kiwiregistry.exception.DuplicateRegistryEntryException

class RegistryContributionBuilder<K, V> internal constructor(
    private val registryKey: RegistryKey<K, V>
) {

    internal val entries = LinkedHashMap<K, V>()

    fun add(key: K, value: V) {
        if (key in entries) {
            throw DuplicateRegistryEntryException(registryKey.id.toString(), key)
        }

        entries[key] = value
    }
}
