# 🥝 kiwi-registry

![GitHub License](https://img.shields.io/github/license/KiwiWhale/kiwi-registry?style=flat-square) 
![Maven Repository](https://img.shields.io/badge/Maven-Reposilite-blue?style=flat-square)

**A lightweight, type-safe, and immutable registry library for Kotlin.**

kiwi-registry is a Kotlin-first library for defining, building, and managing typed registries.

It provides a simple API for organizing entries into namespaces, composing registries through contributors, 
and accessing immutable registry collections through a unified registry graph.

> [!WARNING]
> Early development. APIs and implementation are subject to change.

## Why kiwi-registry?

kiwi-registry focuses on providing a predictable and type-safe registry system without unnecessary complexity.

- **Kotlin-first:** Built around Kotlin's type system, generics, and DSL capabilities.
- **Type-safe:** Registry keys preserve key and value type information.
- **Immutable:** Built registries expose read-only snapshots of their entries.
- **Composable:** Contributors allow independent components to contribute registry entries.
- **Thread-safe provider:** Registry graphs can be replaced and updated atomically.
- **Lightweight:** Minimal dependencies and a straightforward API.

## Features

- Typed registry keys with generic type information
- Namespace-based registry identifiers
- Immutable registries and registry graphs
- DSL-based registry construction
- Contributor-based registry composition
- Duplicate entry detection
- Registry type mismatch detection
- Atomic registry replacement and updates
- Ordered entry storage

## Installation

Add the KiwiWhale Maven repository to your `build.gradle.kts`:

```kotlin
repositories {
    maven("https://repo.mrjimin.xyz/releases")
}

dependencies {
    implementation("io.github.kiwiwhale:kiwi-registry:2.0.0")
}
```

## Quick Start

### Create a Registry

Define a registry key with its namespace, name, and types.

```kotlin
data class User(
    val name: String,
    val level: Int
)

val USER_REGISTRY = registryKey<String, User>(
    "example",
    "users"
)
```

### Build a Registry

Use `registryGraph` to register entries and build an immutable registry graph.

```kotlin
val graph = registryGraph {
    registry(USER_REGISTRY) {
        add("admin", User("Administrator", 100))
        add("guest", User("Guest", 1))
    }
}
```

### Access Entries

Retrieve the registry and access its entries.

```kotlin
val users = graph[USER_REGISTRY]

println(users["admin"]) // User(name=Administrator, level=100)
println(users.size) // 2
```

## Building

Clone the repository:

```bash
git clone https://github.com/KiwiWhale/kiwi-registry.git
cd kiwi-registry
```

Build the project:

```bash
./gradlew build
```

Run tests:

```bash
./gradlew test
```

## Documentation

API documentation is available through GitHub Pages.

## License

kiwi-registry is licensed under the [Apache License 2.0](LICENSE).