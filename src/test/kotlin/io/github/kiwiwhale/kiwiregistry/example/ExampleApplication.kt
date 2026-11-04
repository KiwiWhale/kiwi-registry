package io.github.kiwiwhale.kiwiregistry.example

import io.github.kiwiwhale.kiwiregistry.core.ConcurrentRegistry

data class User(
    val id: String,
    val email: String
)

fun main() {

    val registry = ConcurrentRegistry<User, ExampleKey>(
        keyFactory = ::ExampleKey
    )

    val userKey = ExampleKey("user")

    val user = User(
        id = "user-001",
        email = "hello@example.com"
    )

    // Typed Key registration
    registry.register(userKey, user)

    // Typed Key lookup
    val result1 = registry.get(userKey)

    println(result1)

    // String lookup
    val result2 = registry.get("user")

    println(result2)

    // Check
    println(registry.contains("user"))

    // Snapshot
    println(registry.snapshot())

    // Unregister
    registry.unregister(userKey)

    println(registry.size)
}