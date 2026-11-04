package io.github.kiwiwhale.kiwiregistry.example

import io.github.kiwiwhale.kiwiregistry.KiwiRegistry
import io.github.kiwiwhale.kiwiregistry.RegistryId

fun main() {

    // Create registry
    val registry = KiwiRegistry<User>("example")

    val user = User(
        id = "user-001",
        name = "Kiwi",
        email = "hello@example.com"
    )

    // Registration using String
    registry.register("user", user)

    // Registration using RegistryKey
    registry.register(UserKey, user)

    // Registration using RegistryId
    registry.register(
        RegistryId("example", "admin"),
        user.copy(id = "user-002", name = "Admin")
    )

    // Get using String
    val user1 = registry["user"]

    // Get using RegistryKey
    val user2 = registry[UserKey]

    // Get using RegistryId
    val user3 = registry[
        RegistryId("example", "user")
    ]

    println(user1)
    println(user2)
    println(user3)

    // Contains
    println("user" in registry)
    println(UserKey in registry)

    // Size
    println(registry.size)

    // Get or null
    val missing = registry.getOrNull("missing")
    println(missing)

    // Register if absent
    registry.registerIfAbsent(
        "guest",
        User("user-003", "Guest", "guest@example.com")
    )

    // Replace
    registry.replace(
        "user",
        user.copy(name = "Updated Kiwi")
    )

    // Snapshot
    val snapshot = registry.snapshot()
    println(snapshot)

    // Unregister
    registry.unregister("user")

    println(registry.size)
}
