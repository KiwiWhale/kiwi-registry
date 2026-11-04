package io.github.kiwiwhale.kiwiregistry.exception

/**
 * Thrown when attempting to register an existing registry entry.
 */
class DuplicateRegistryEntryException(
    val duplicateIds: List<String>
) : IllegalStateException(
    "Duplicate registry entries: ${duplicateIds.joinToString()}."
) {
    constructor(id: String) : this(listOf(id))
}
