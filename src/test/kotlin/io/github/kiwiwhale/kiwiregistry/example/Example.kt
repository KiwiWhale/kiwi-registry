package io.github.kiwiwhale.kiwiregistry.example

import io.github.kiwiwhale.kiwiregistry.builder.RegistryContributor
import io.github.kiwiwhale.kiwiregistry.core.RegistryGraph
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

val DEVELOPER_USERS = RegistryContributor("developer-users") {
    registry(USER_REGISTRY) {
        add("developer", User("Developer", 50))
    }
}

fun main() {
    val graph = registryGraph {
        contribute(DEFAULT_USERS)
        contribute(DEVELOPER_USERS)

        validate {
            requireNotEmpty(USER_REGISTRY)

            validateEntries(
                USER_REGISTRY,
                message = { id, user ->
                    "User '$id' has an invalid level: ${user.level}"
                }
            ) { _, user ->
                user.level > 0
            }
        }
    }

    val users = graph[USER_REGISTRY]

    println("Registered users:")

    users.data.forEach { (id, user) ->
        println("$id -> ${user.name} (level ${user.level})")
    }

    println("Total users: ${users.size}")

    val provider = RegistryProvider(graph)

    println("Current graph size: ${provider.graph.size}")

    val updated = registryGraph {
        registry(USER_REGISTRY) {
            add("moderator", User("Moderator", 75))
        }
    }

    provider.update { current ->
        RegistryGraph.merge(current, updated)
    }

    println("Updated user count: ${provider.graph[USER_REGISTRY].size}")
}
