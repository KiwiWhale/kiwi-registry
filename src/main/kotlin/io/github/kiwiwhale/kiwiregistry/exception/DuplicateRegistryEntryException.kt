package io.github.kiwiwhale.kiwiregistry.exception

class DuplicateRegistryEntryException(
    id: String
) : RegistryException("Registry entry '$id' is already registered.")
