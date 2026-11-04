package io.github.kiwiwhale.kiwiregistry.bootstrap

import io.github.kiwiwhale.kiwiregistry.RegistryId
import io.github.kiwiwhale.kiwiregistry.RegistryKey
import io.github.kiwiwhale.kiwiregistry.core.FrozenRegistry
import io.github.kiwiwhale.kiwiregistry.exception.RegistryNotFoundException
import java.util.Collections
import kotlin.reflect.KClass

class Bootstrap internal constructor(
    registries: Map<RegistryId, RegisteredRegistry>
) {
    private val registries =
        Collections.unmodifiableMap(
            LinkedHashMap(registries)
        )

    val size: Int
        get() = registries.size

    val ids: Set<RegistryId>
        get() = registries.keys

    operator fun contains(id: RegistryId): Boolean =
        id in registries

    operator fun <T : Any> get(
        key: RegistryKey<T>
    ): FrozenRegistry<T> =
        getOrNull(key)
            ?: throw RegistryNotFoundException(key.id)

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getOrNull(
        key: RegistryKey<T>
    ): FrozenRegistry<T>? {
        val registered = registries[key.id]
            ?: return null

        require(registered.type == key.type) {
            "Registry type mismatch for '${key.id}'. " +
                    "Expected '${registered.type}', received '${key.type}'."
        }

        return registered.registry as FrozenRegistry<T>
    }

    companion object {
        fun create(
            configure: BootstrapBuilder.() -> Unit
        ): Bootstrap {
            val builder = BootstrapBuilder()

            builder.configure()
            builder.markBuilt()

            val result =
                LinkedHashMap<RegistryId, RegisteredRegistry>()

            builder.registries.forEach { (id, registry) ->
                result[id] = RegisteredRegistry(
                    type = registry.type,
                    registry = registry.freeze()
                )
            }

            return Bootstrap(result)
        }
    }

    internal class RegisteredRegistry(
        val type: KClass<*>,
        val registry: FrozenRegistry<*>
    )
}
