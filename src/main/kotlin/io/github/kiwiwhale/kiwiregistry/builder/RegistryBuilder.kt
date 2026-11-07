package io.github.kiwiwhale.kiwiregistry.builder

import io.github.kiwiwhale.kiwiregistry.core.Registry
import io.github.kiwiwhale.kiwiregistry.core.RegistryGraph
import io.github.kiwiwhale.kiwiregistry.core.RegistryId
import io.github.kiwiwhale.kiwiregistry.core.RegistryKey
import io.github.kiwiwhale.kiwiregistry.exception.RegistryBuildException
import io.github.kiwiwhale.kiwiregistry.exception.RegistryTypeMismatchException
import io.github.kiwiwhale.kiwiregistry.internal.DefaultRegistry
import java.util.Collections

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

    private val executedContributorIds =
        LinkedHashSet<String>()

    private var currentContributor: String? = null

    private var activeRegistryBlock = false

    private var state = State.OPEN

    /**
     * Ensures that the builder is still available for modification.
     *
     * @throws IllegalStateException If the builder is building or has been built.
     */
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

    /**
     * Ensures that the builder can start building a registry graph.
     *
     * @throws IllegalStateException If a configuration block is executing.
     */
    private fun checkCanBuild() {
        checkOpen()

        check(currentContributor == null) {
            "Cannot build while a contributor is executing."
        }

        check(!activeRegistryBlock) {
            "Cannot build while a registry block is executing."
        }
    }

    /**
     * Creates an independent copy of all configured registries.
     *
     * @param source Registries to copy.
     * @return Mutable copy preserving registry and entry insertion order.
     */
    private fun copyRegistries(
        source: Map<RegistryId, MutableEntries>
    ): LinkedHashMap<RegistryId, MutableEntries> =
        LinkedHashMap<RegistryId, MutableEntries>().also { copy ->
            source.forEach { (id, entries) ->
                copy[id] = entries.copy()
            }
        }

    /**
     * Validates the runtime types of an existing registry against a key.
     *
     * @param existing Existing registry entries, if present.
     * @param key Key to validate.
     * @throws RegistryTypeMismatchException If the registry types differ.
     */
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
     * Nested contributors and contributors inside registry blocks
     * are not supported.
     *
     * @param id Unique contributor identifier.
     * @param block Configuration to execute.
     * @throws IllegalArgumentException If the ID is blank or contains surrounding whitespace.
     * @throws IllegalStateException If the contributor is duplicated or execution is nested.
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

        check(id !in executedContributorIds) {
            "Duplicate contributor: '$id'."
        }

        val previousRegistries = registries

        registries = copyRegistries(previousRegistries)
        currentContributor = id

        try {
            block()
            executedContributorIds.add(id)
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
     * Registry configuration blocks cannot be nested.
     *
     * @param key Typed identifier of the target registry.
     * @param block Entry configuration.
     * @throws RegistryTypeMismatchException If an existing registry has incompatible types.
     * @throws RegistryBuildException If entry configuration fails.
     * @throws IllegalStateException If another registry block is executing.
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

        val workingEntries = LinkedHashMap<Any, Any>(
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
     * Checks whether a registry with the specified ID exists.
     *
     * @param id Registry identifier.
     * @return True if the registry exists.
     */
    fun containsRegistry(id: RegistryId): Boolean =
        id in registries

    /**
     * Checks whether a registry with the specified key's ID exists.
     *
     * This operation checks only the ID and does not verify
     * runtime type compatibility.
     *
     * @param key Typed registry identifier.
     * @return True if the registry exists.
     */
    fun containsRegistry(key: RegistryKey<*, *>): Boolean =
        containsRegistry(key.id)

    /**
     * Returns the number of entries in the specified registry.
     *
     * @param id Registry identifier.
     * @return Entry count, or null if the registry does not exist.
     */
    fun getEntryCount(id: RegistryId): Int? =
        registries[id]?.entries?.size

    /**
     * Returns the number of entries in the specified registry.
     *
     * The runtime types of the supplied key must match the
     * registered registry.
     *
     * @param key Typed registry identifier.
     * @return Entry count, or null if the registry does not exist.
     * @throws RegistryTypeMismatchException If runtime types do not match.
     */
    fun getEntryCount(key: RegistryKey<*, *>): Int? {
        val existing = registries[key.id] ?: return null

        validateRegistryKey(existing, key)

        return existing.entries.size
    }

    /**
     * Removes the specified registry.
     *
     * The registry's entries are removed together with it.
     *
     * @param key Typed registry identifier.
     * @return True if a registry was removed, or false if absent.
     * @throws RegistryTypeMismatchException If the registered types differ.
     * @throws IllegalStateException If the builder is not open.
     */
    fun removeRegistry(key: RegistryKey<*, *>): Boolean {
        checkOpen()

        val existing = registries[key.id] ?: return false

        validateRegistryKey(existing, key)

        registries.remove(key.id)

        return true
    }

    /**
     * Removes all entries from the specified registry.
     *
     * The registry itself remains registered.
     *
     * @param key Typed registry identifier.
     * @return Number of removed entries, or null if the registry does not exist.
     * @throws RegistryTypeMismatchException If the registered types differ.
     * @throws IllegalStateException If the builder is not open.
     */
    fun clearRegistry(key: RegistryKey<*, *>): Int? {
        checkOpen()

        val existing = registries[key.id] ?: return null

        validateRegistryKey(existing, key)

        val removedCount = existing.entries.size

        existing.entries.clear()

        return removedCount
    }

    /**
     * Builds an immutable [RegistryGraph] from the configured registries.
     *
     * A successful build permanently closes this builder.
     * If building fails, the builder remains open.
     *
     * @return Immutable registry graph.
     * @throws IllegalStateException If the builder is already building,
     * has been built, or a configuration block is executing.
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
                val key = mutable.key as RegistryKey<Any, Any>

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
        get() = executedContributorIds.size

    /**
     * IDs of successfully executed contributors in execution order.
     *
     * Returns an immutable snapshot.
     */
    val contributorIds: Set<String>
        get() = Collections.unmodifiableSet(
            LinkedHashSet(executedContributorIds)
        )

    /**
     * Checks whether a contributor has been successfully executed.
     *
     * @param id Contributor identifier.
     * @return True if the contributor was successfully executed.
     */
    fun hasContributor(id: String): Boolean =
        id in executedContributorIds
}
