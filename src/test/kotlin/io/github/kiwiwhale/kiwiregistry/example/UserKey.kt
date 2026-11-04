package io.github.kiwiwhale.kiwiregistry.example

import io.github.kiwiwhale.kiwiregistry.RegistryId
import io.github.kiwiwhale.kiwiregistry.RegistryKey

object UserKey : RegistryKey<User> {
    override val id = RegistryId("example", "user")
}

data class User(
    val id: String,
    val name: String,
    val email: String
)