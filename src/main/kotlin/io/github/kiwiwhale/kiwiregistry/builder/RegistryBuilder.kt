package io.github.kiwiwhale.kiwiregistry.builder

import io.github.kiwiwhale.kiwiregistry.core.Registry
import io.github.kiwiwhale.kiwiregistry.core.RegistryGraph
import io.github.kiwiwhale.kiwiregistry.core.RegistryId
import io.github.kiwiwhale.kiwiregistry.core.RegistryKey
import io.github.kiwiwhale.kiwiregistry.exception.RegistryBuildException
import io.github.kiwiwhale.kiwiregistry.exception.RegistryTypeMismatchException
import io.github.kiwiwhale.kiwiregistry.internal.DefaultRegistry

/**
 * Mutable builder used to construct an immutable [RegistryGraph].
 *
 * Registry modifications are performed transactionally.
 * Failed registry configurations and contributors do not commit their changes.
 *
 * This builder is not thread-safe.
 * All operations must be performed from a single thread.
 *
 * A successful [build] permanently closes this builder.
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
     * All registry changes made during the block are committed
     * only if the block completes successfully.
     *
     * A failed contributor is rolled back completely, and its ID
     * remains available for future use.
     *
     * External side effects are not rolled back.
     *
     * Nested contributors and execution inside registry blocks
     * are not supported.
     *
     * @param id Unique contributor identifier.
     * @param block Configuration to execute.
     *
     * @throws IllegalArgumentException If the ID is blank or contains
     * surrounding whitespace.
     * @throws IllegalStateException If the builder is not open,
     * another contributor is executing, or a registry block is active.
     * @throws IllegalStateException If the contributor ID is duplicated.
     */
    fun contributor(
        id: String,
        block: RegistryBuilder.() -> Unit
    ) {
        checkOpen()

        require(id.isNotBlank()) {
            "Contributor ID cannot be blank."
        }

        require(id == id.trim()) {
            "Contributor ID cannot contain surrounding whitespace."
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

    /**
     * Applies a reusable [RegistryContributor].
     *
     * The contributor is executed using the same transactional
     * behavior as [contributor].
     *
     * @param contributor Contributor to apply.
     */
    fun contribute(contributor: RegistryContributor) {
        checkOpen()
        contributor.applyTo(this)
    }

    /**
     * Configures entries for a registry.
     *
     * Existing entries are preserved.
     * New entries are committed only after successful completion
     * of the configuration block.
     *
     * If the block fails, its changes are discarded.
     *
     * Registry blocks cannot be nested.
     *
     * @param key Typed identifier of the target registry.
     * @param block Entry configuration.
     *
     * @throws IllegalStateException If the builder is not open
     * or another registry block is executing.
     * @throws RegistryTypeMismatchException If the registry ID
     * already exists with incompatible key or value types.
     * @throws RegistryBuildException If an entry configuration fails
     * due to a registry-specific build error.
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
     * Builds an immutable [RegistryGraph] from the configured registries.
     *
     * The resulting graph contains independent snapshots of the
     * configured registry entries.
     *
     * A successful build permanently closes this builder.
     * If building fails, the builder remains open and can be used again.
     *
     * Building is not allowed while a contributor or registry block
     * is executing.
     *
     * @return Immutable registry graph.
     *
     * @throws IllegalStateException If the builder is already building,
     * has already been built, or a configuration block is executing.
     * @throws RegistryBuildException If graph construction fails.
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

    /**
     * Whether this builder has successfully built a graph.
     *
     * Once true, the builder cannot be used for further configuration.
     */
    val isBuilt: Boolean
        get() = state == State.BUILT

    /**
     * Number of registries currently configured.
     */
    val registryCount: Int
        get() = registries.size

    /**
     * Number of successfully executed contributors.
     */
    val contributorCount: Int
        get() = contributorIds.size
}
