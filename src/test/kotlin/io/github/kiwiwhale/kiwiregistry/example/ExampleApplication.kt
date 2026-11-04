package io.github.kiwiwhale.kiwiregistry.example

import io.github.kiwiwhale.kiwiregistry.KiwiRegistry
import io.github.kiwiwhale.kiwiregistry.RegistryId

fun main() {

    val registry = KiwiRegistry<User, UserKey>(
        keyFactory = ::UserKey
    )

    val user = User(
        id = "user-001",
        name = "Kiwi",
        email = "hello@example.com"
    )

    // Registration
    registry.register(UserKey.USER, user)

    // Get using typed key
    val user1 = registry[UserKey.USER]

    // Get using RegistryId
    val user2 = registry[
        RegistryId("example", "user")
    ]

    // Get using get()
    val user3 = registry[UserKey.USER]

    println(user1)
    println(user2)
    println(user3)

    // Contains
    println(UserKey.USER in registry)

    // Size
    println(registry.size)

    // Snapshot
    println(registry.snapshot())

    // Unregister
    registry.unregister(UserKey.USER)

    println(registry.size)
}