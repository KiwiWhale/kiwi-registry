package io.github.kiwiwhale.kiwiregistry.example

import io.github.kiwiwhale.kiwiregistry.RegistryKey

data class ExampleKey(
    override val name: String
) : RegistryKey