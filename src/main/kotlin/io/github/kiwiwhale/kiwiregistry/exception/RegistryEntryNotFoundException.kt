package io.github.kiwiwhale.kiwiregistry.exception

class RegistryEntryNotFoundException(
    name: String
) : RegistryException("Registry entry '$name' was not found.")
