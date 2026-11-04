package io.github.kiwiwhale.kiwiregistry.exception

class RegistryEntryNotFoundException(
    id: String
) : RegistryException("Registry entry '$id' was not found.")
