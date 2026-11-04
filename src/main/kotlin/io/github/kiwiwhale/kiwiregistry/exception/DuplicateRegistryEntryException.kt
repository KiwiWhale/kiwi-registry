package io.github.kiwiwhale.kiwiregistry.exception

class DuplicateRegistryEntryException(
    name: String
) : RegistryException("Registry entry '$name' is already registered.")
