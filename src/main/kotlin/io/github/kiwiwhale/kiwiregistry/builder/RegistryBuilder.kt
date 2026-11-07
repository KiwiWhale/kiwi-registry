package io.github.kiwiwhale.kiwiregistry.builder

import io.github.kiwiwhale.kiwiregistry.core.Registry
import io.github.kiwiwhale.kiwiregistry.core.RegistryGraph
import io.github.kiwiwhale.kiwiregistry.core.RegistryId
import io.github.kiwiwhale.kiwiregistry.core.RegistryKey
import io.github.kiwiwhale.kiwiregistry.exception.RegistryBuildException
import io.github.kiwiwhale.kiwiregistry.exception.RegistryTypeMismatchException
import io.github.kiwiwhale.kiwiregistry.internal.DefaultRegistry

/**
 * Mutable builder used to construct an immutable RegistryGraph.
 *
 * This builder is not thread-safe.
 *
 * All configuration must be performed from a single thread.
 */
class RegistryBuilder internal constructor() {

    private class MutableEntries(
        val key: RegistryKey<*, *>,
        val entries: LinkedHashMap<Any, Any> = LinkedHashMap()
    ) {
        fun copy(): MutableEntries =
            MutableEntries(key).also {
                it.entries.putAll(entries)
            }
    }

    private enum class State {
        OPEN,
        BUILDING,
        BUILT
    }

    private var registries =
        LinkedHashMap<RegistryId, MutableEntries>()

    private val contributorIds =
        LinkedHashSet<String>()

    private var currentContributor: String? = null

    private var activeRegistryBlock = false

    private var state = State.OPEN

    private fun checkOpen() {
        check(state == State.OPEN) {
            when (state) {
                State.BUILDING ->
                    "RegistryBuilder is currently building."

                State.BUILT ->
                    "RegistryBuilder has already been built."

                State.OPEN -> ""
            }
        }
    }

    private fun checkCanBuild() {
        checkOpen()

        check(currentContributor == null) {
            "Cannot build while a contributor is executing."
        }

        check(!activeRegistryBlock) {
            "Cannot build while a registry block is executing."
        }
    }

    private fun copyRegistries(
        source: Map<RegistryId, MutableEntries>
    ): LinkedHashMap<RegistryId, MutableEntries> =
        LinkedHashMap<RegistryId, MutableEntries>().also { copy ->
            source.forEach { (id, entries) ->
                copy[id] = entries.copy()
            }
        }

    private fun validateRegistryKey(
        existing: MutableEntries?,
        key: RegistryKey<*, *>
    ) {
        if (existing != null && !existing.key.hasSameTypes(key)) {
            throw RegistryTypeMismatchException(key.id)
        }
    }

    /**
     * Executes a contributor as an isolated transaction.
     *
     * Registry changes are rolled back if the block fails.
     * External side effects are not rolled back.
     */
    fun contributor(
        id: String,
        block: RegistryBuilder.() -> Unit
    ) {
        checkOpen()

        require(id.isNotBlank()) {
            "Contributor ID cannot be blank."
        }

        check(!activeRegistryBlock) {
            "Cannot execute a contributor inside a registry block."
        }

        check(currentContributor == null) {
            "Nested contributors are not supported."
        }

        check(id !in contributorIds) {
            "Duplicate contributor: '$id'."
        }

        val previousRegistries = registries
        val workingRegistries = copyRegistries(previousRegistries)

        registries = workingRegistries
        currentContributor = id

        try {
            block()
            contributorIds.add(id)
        } catch (exception: Throwable) {
            registries = previousRegistries
            throw exception
        } finally {
            currentContributor = null
        }
    }

    fun contribute(contributor: RegistryContributor) {
        checkOpen()
        contributor.applyTo(this)
    }

    /**
     * Configures entries for a registry.
     *
     * Changes are committed only after successful completion.
     */
    fun <K : Any, V : Any> registry(
        key: RegistryKey<K, V>,
        block: RegistryEntries<K, V>.() -> Unit
    ) {
        checkOpen()

        check(!activeRegistryBlock) {
            "Nested registry blocks are not supported."
        }

        val existing = registries[key.id]

        validateRegistryKey(existing, key)

        val workingEntries =
            LinkedHashMap<Any, Any>(
                existing?.entries ?: emptyMap()
            )

        val registryEntries = RegistryEntries(
            key = key,
            entries = workingEntries,
            contributor = currentContributor ?: "anonymous"
        )

        activeRegistryBlock = true

        try {
            registryEntries.block()

            val updated = existing?.copy()
                ?: MutableEntries(key)

            updated.entries.clear()
            updated.entries.putAll(workingEntries)

            registries[key.id] = updated
        } finally {
            registryEntries.close()
            activeRegistryBlock = false
        }
    }

    /**
     * Builds an immutable RegistryGraph.
     *
     * Successful builds permanently close this builder.
     * Failed builds leave it open.
     */
    @Suppress("UNCHECKED_CAST")
    fun build(): RegistryGraph {
        checkCanBuild()

        state = State.BUILDING

        try {
            val result =
                LinkedHashMap<RegistryId, Registry<*, *>>()

            for ((id, mutable) in registries) {
                val key =
                    mutable.key as RegistryKey<Any, Any>

                result[id] = DefaultRegistry(
                    key = key,
                    source = LinkedHashMap(mutable.entries)
                )
            }

            val graph = RegistryGraph(result)

            state = State.BUILT

            return graph
        } catch (exception: Throwable) {
            state = State.OPEN

            if (exception is Error) {
                throw exception
            }

            if (exception is RegistryBuildException) {
                throw exception
            }

            throw RegistryBuildException(
                "Failed to build RegistryGraph.",
                exception
            )
        }
    }

    val isBuilt: Boolean
        get() = state == State.BUILT

    val registryCount: Int
        get() = registries.size

    val contributorCount: Int
        get() = contributorIds.size
}
