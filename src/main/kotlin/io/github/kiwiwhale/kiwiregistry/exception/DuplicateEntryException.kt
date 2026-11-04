package io.github.kiwiwhale.kiwiregistry.exception

class DuplicateEntryException(
    val registryId: String,
    val duplicateKeys: List<String>
) : RegistryException(
    "Duplicate entries in registry '$registryId': " +
            duplicateKeys.joinToString()
) {
    constructor(
        registryId: String,
        key: String
    ) : this(registryId, listOf(key))
}