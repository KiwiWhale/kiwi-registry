package io.github.kiwiwhale.kiwiregistry.builder

import io.github.kiwiwhale.kiwiregistry.core.Registry
import io.github.kiwiwhale.kiwiregistry.core.RegistryGraph
import io.github.kiwiwhale.kiwiregistry.core.RegistryId
import io.github.kiwiwhale.kiwiregistry.core.RegistryKey
import io.github.kiwiwhale.kiwiregistry.dsl.RegistryValidationScope
import io.github.kiwiwhale.kiwiregistry.exception.RegistryBuildException
import io.github.kiwiwhale.kiwiregistry.exception.RegistryTypeMismatchException
import io.github.kiwiwhale.kiwiregistry.exception.RegistryValidationException
import io.github.kiwiwhale.kiwiregistry.internal.DefaultRegistry
import io.github.kiwiwhale.kiwiregistry.validation.RegistryValidator
import java.util.Collections

/**
 * Mutable builder used to construct an immutable [RegistryGraph].
 *
 * Registry modifications are performed transactionally.
 * Failed registry configurations and contributors do not commit
 * their changes.
 *
 * This builder is not thread-safe.
 * All operations must be performed from a single thread.
 *
 * A successful [build] permanently closes this builder.
 *
 * @see RegistryEntries
 * @see RegistryContributor
 */
class RegistryBuilder internal constructor() {

    private class MutableEntries(
        val key: RegistryKey<*, *>,
        val entries: LinkedHashMap<Any, Any> = LinkedHashMap()
    ) {
        fun copy(): MutableEntries =
            MutableEntries(key).also { copy ->
                copy.entries.putAll(entries)
            }
    }

    private enum class State {
        OPEN,
        BUILDING,
        BUILT
    }

    private var state = State.OPEN

    private var registries =
        LinkedHashMap<RegistryId, MutableEntries>()

    private val executedContributorIds =
        LinkedHashSet<String>()

    private val validationBlocks =
        mutableListOf<RegistryValidationScope.() -> Unit>()

    private var currentContributor: String? = null

    private var activeRegistryBlock = false

    /**
     * Ensures that the builder is available for modification.
     *
     * @throws IllegalStateException If the builder is building
     * or has already been built.
     */
    private fun checkOpen() {
        check(state == State.OPEN) {
            when (state) {
                State.BUILDING ->
                    "RegistryBuilder is currently building."

                State.BUILT ->
                    "RegistryBuilder has already been built."

                State.OPEN ->
                    "RegistryBuilder is open."
            }
        }
    }

    /**
     * Prevents builder-level operations while a registry
     * configuration block is executing.
     *
     * RegistryEntries remains the only supported mutation
     * interface inside a registry block.
     */
    private fun checkNoActiveRegistryBlock() {
        check(!activeRegistryBlock) {
            "Cannot perform this operation inside a registry block."
        }
    }

    /**
     * Ensures that a registry graph can be built.
     */
    private fun checkCanBuild() {
        checkOpen()
        checkNoActiveRegistryBlock()

        check(currentContributor == null) {
            "Cannot build while a contributor is executing."
        }
    }

    /**
     * Creates an independent mutable snapshot of the registries.
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
     * Validates the runtime types of an existing registry.
     *
     * @throws RegistryTypeMismatchException If the types differ.
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
     * All registry changes are committed only if the
     * configuration block completes successfully.
     *
     * A failed contributor is rolled back completely.
     * Its ID remains available for future execution.
     *
     * External side effects cannot be rolled back.
     *
     * Nested contributors and contributors inside registry
     * configuration blocks are not supported.
     *
     * @param id Unique contributor identifier.
     * @param block Configuration to execute.
     *
     * @throws IllegalArgumentException If the ID is invalid.
     * @throws IllegalStateException If execution is duplicated
     * or nested.
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

        checkNoActiveRegistryBlock()

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
     * New entries are committed only after successful
     * completion of the configuration block.
     *
     * Registry configuration blocks cannot be nested.
     *
     * @param key Typed identifier of the target registry.
     * @param block Entry configuration.
     *
     * @throws RegistryTypeMismatchException If registry types differ.
     * @throws IllegalStateException If the builder is unavailable
     * or another registry block is executing.
     */
    fun <K : Any, V : Any> registry(
        key: RegistryKey<K, V>,
        block: RegistryEntries<K, V>.() -> Unit
    ) {
        checkOpen()
        checkNoActiveRegistryBlock()

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
     * This operation does not modify the builder.
     */
    fun containsRegistry(id: RegistryId): Boolean =
        registries.containsKey(id)

    /**
     * Checks whether a registry with the specified key exists.
     *
     * Only the ID is checked. Runtime types are not verified.
     */
    fun containsRegistry(key: RegistryKey<*, *>): Boolean =
        containsRegistry(key.id)

    /**
     * Returns the number of entries in a registry.
     *
     * @return Entry count, or null if absent.
     */
    fun getEntryCount(id: RegistryId): Int? =
        registries[id]?.entries?.size

    /**
     * Returns the number of entries in a registry.
     *
     * Runtime types must match the registered registry.
     *
     * @throws RegistryTypeMismatchException If types differ.
     */
    fun getEntryCount(key: RegistryKey<*, *>): Int? {
        val existing = registries[key.id] ?: return null

        validateRegistryKey(existing, key)

        return existing.entries.size
    }

    /**
     * Removes a registry and all its entries.
     *
     * @return True if the registry was removed.
     */
    fun removeRegistry(key: RegistryKey<*, *>): Boolean {
        checkOpen()
        checkNoActiveRegistryBlock()

        val existing = registries[key.id] ?: return false

        validateRegistryKey(existing, key)

        registries.remove(key.id)

        return true
    }

    /**
     * Removes all entries from a registry.
     *
     * The registry itself remains registered.
     *
     * @return Number of removed entries, or null if absent.
     */
    fun clearRegistry(key: RegistryKey<*, *>): Int? {
        checkOpen()
        checkNoActiveRegistryBlock()

        val existing = registries[key.id] ?: return null

        validateRegistryKey(existing, key)

        val removedCount = existing.entries.size

        existing.entries.clear()

        return removedCount
    }

    /**
     * Registers validation rules to execute during build.
     *
     * Validation blocks are executed in registration order.
     *
     * @param block Validation configuration.
     *
     * @throws IllegalStateException If a contributor or
     * registry configuration block is executing.
     */
    fun validate(
        block: RegistryValidationScope.() -> Unit
    ) {
        checkOpen()
        checkNoActiveRegistryBlock()

        check(currentContributor == null) {
            "Cannot register validation inside a contributor."
        }

        validationBlocks.add(block)
    }

    /**
     * Creates immutable registry snapshots.
     */
    @Suppress("UNCHECKED_CAST")
    private fun createRegistries(): LinkedHashMap<RegistryId, Registry<*, *>> {
        val result =
            LinkedHashMap<RegistryId, Registry<*, *>>()

        for ((id, mutable) in registries) {
            val key = mutable.key as RegistryKey<Any, Any>

            result[id] = DefaultRegistry(
                key = key,
                source = LinkedHashMap(mutable.entries)
            )
        }

        return result
    }

    /**
     * Executes all registered validation rules.
     *
     * @throws RegistryValidationException If validation fails.
     */
    private fun validateRegistries(
        registries: Map<RegistryId, Registry<*, *>>
    ) {
        if (validationBlocks.isEmpty()) {
            return
        }

        val validator = RegistryValidator(registries)

        for (block in validationBlocks) {
            validator.validate(block)
        }

        val result = validator.result()

        if (result.hasErrors) {
            throw RegistryValidationException(result)
        }
    }

    /**
     * Builds an immutable [RegistryGraph].
     *
     * A successful build permanently closes this builder.
     *
     * If construction or validation fails, the builder
     * returns to the OPEN state and can be modified or rebuilt.
     *
     * @return Immutable registry graph.
     *
     * @throws IllegalStateException If the builder is already
     * building, built, or executing a configuration block.
     * @throws RegistryBuildException If graph construction fails.
     * @throws RegistryValidationException If validation fails.
     */
    fun build(): RegistryGraph {
        checkCanBuild()

        state = State.BUILDING

        var succeeded = false

        try {
            val builtRegistries = createRegistries()

            validateRegistries(builtRegistries)

            val graph = RegistryGraph(builtRegistries)

            succeeded = true

            return graph
        } catch (exception: RegistryBuildException) {
            throw exception
        } catch (exception: RegistryValidationException) {
            throw exception
        } catch (exception: Exception) {
            throw RegistryBuildException(
                "Failed to build RegistryGraph.",
                exception
            )
        } finally {
            state = if (succeeded) {
                State.BUILT
            } else {
                State.OPEN
            }
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
     */
    fun hasContributor(id: String): Boolean =
        id in executedContributorIds
}
