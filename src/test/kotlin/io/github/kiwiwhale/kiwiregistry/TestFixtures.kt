package io.github.kiwiwhale.kiwiregistry

import io.github.kiwiwhale.kiwiregistry.builder.RegistryContributor
import io.github.kiwiwhale.kiwiregistry.core.registryKey

data class User(
    val name: String,
    val level: Int
)

data class Product(
    val name: String,
    val price: Int
)

val USER_REGISTRY = registryKey<String, User>(
    "example",
    "users"
)

val PRODUCT_REGISTRY = registryKey<String, Product>(
    "example",
    "products"
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