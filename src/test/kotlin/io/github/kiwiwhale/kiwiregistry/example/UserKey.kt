package io.github.kiwiwhale.kiwiregistry.example

import io.github.kiwiwhale.kiwiregistry.RegistryId
import io.github.kiwiwhale.kiwiregistry.RegistryKey

data class UserKey(
    override val id: RegistryId
) : RegistryKey<User> {

    companion object {
        val USER = UserKey(
            RegistryId("example", "user")
        )
    }
}

data class User(
    val id: String,
    val name: String,
    val email: String
)