package io.github.kiwiwhale.kiwiregistry.example

import io.github.kiwiwhale.kiwiregistry.builder.RegistryContributor
import io.github.kiwiwhale.kiwiregistry.core.registryKey
import io.github.kiwiwhale.kiwiregistry.dsl.registryGraph
import io.github.kiwiwhale.kiwiregistry.provider.RegistryProvider

data class User(
    val name: String,
    val level: Int
)

val USER_REGISTRY = registryKey<String, User>(
    "example",
    "users"
)

val DEFAULT_USERS = RegistryContributor("default-users") {
    registry(USER_REGISTRY) {
        add("admin", User("Administrator", 100))
        add("guest", User("Guest", 1))
    }
}

val graph = registryGraph {
    contribute(DEFAULT_USERS)

    contributor("custom-users") {
        registry(USER_REGISTRY) {
            add("developer", User("Developer", 50))
        }
    }
}

fun main() {
    val users = graph[USER_REGISTRY]

    println(users["admin"])
    println(users["developer"])
    println("Total users: ${users.size}")

    val provider = RegistryProvider(graph)

    println("Current graph: ${provider.graph.size}")
}
