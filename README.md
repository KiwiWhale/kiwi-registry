# 🥝 kiwi-registry

![GitHub License](https://img.shields.io/github/license/KiwiWhale/kiwi-registry?style=flat-square) 
[![Maven Repository](https://img.shields.io/badge/Maven-Reposilite-blue?style=flat-square)](https://repo.mrjimin.xyz/#/releases/io/github/kiwiwhale/kiwi-registry)

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

### Define a registry key

A key carries its namespace, its name, and the key/value types.

```kotlin
data class User(val name: String, val level: Int)
 
val USER_REGISTRY = registryKey<String, User>("example", "users")
```

### Build a registry graph

Use `registryGraph` to register entries and build an immutable graph.

```kotlin
val graph = registryGraph {
    registry(USER_REGISTRY) {
        add("admin", User("Administrator", 100))
        add("guest", User("Guest", 1))
    }
}
```

### Access entries

```kotlin
val users = graph[USER_REGISTRY]
 
println(users["admin"]) // User(name=Administrator, level=100)
println(users.size)     // 2
```

## Contributors

Contributors allow independent modules to contribute entries to the same registry.

```kotlin
val DEFAULT_USERS = RegistryContributor("default-users") {
    registry(USER_REGISTRY) {
        add("admin", User("Administrator", 100))
    }
}
 
val DEVELOPER_USERS = RegistryContributor("developer-users") {
    registry(USER_REGISTRY) {
        add("developer", User("Developer", 50))
    }
}
 
val graph = registryGraph {
    contribute(DEFAULT_USERS)
    contribute(DEVELOPER_USERS)
}
```

Contributor IDs must be unique within a builder.

## Validation

Validate the registry configuration before the graph is built.

```kotlin
val graph = registryGraph {
    registry(USER_REGISTRY) {
        add("admin", User("Administrator", 100))
    }
 
    validate {
        requireNotEmpty(USER_REGISTRY)
 
        validateEntries(
            key = USER_REGISTRY,
            message = { id, _ -> "Invalid user: $id" }
        ) { _, user ->
            user.level > 0
        }
    }
}
```

Validation supports three severity levels:

| Severity  | Behavior                      |
|-----------|-------------------------------|
| `ERROR`   | Prevents graph construction   |
| `WARNING` | Does not prevent construction |
| `INFO`    | Does not prevent construction |

Available operations: `requireRegistry`, `requireEntry`, `requireNotEmpty`,
`validateEntry`, `validateRegistry`, and `validateEntries`.

## RegistryGraph

Multiple graphs can be merged into a new immutable graph.

```kotlin
val merged = RegistryGraph.merge(firstGraph, secondGraph)
```

- When duplicate entry keys exist, values from later graphs take precedence while preserving the original insertion position.
- Original graphs remain unchanged.
- Incompatible registry types are rejected.
## RegistryProvider

`RegistryProvider` provides atomic access and updates to the current registry graph.

```kotlin
val provider = RegistryProvider(graph)
 
// Transform the current graph.
provider.update { current ->
    RegistryGraph.merge(current, anotherGraph)
}
 
// Replace the current graph.
provider.replace(anotherGraph)
```

Compare-and-replace operations are also supported.

> [!NOTE]
> `update` transformations may execute multiple times under contention.
> Avoid external side effects inside them.

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

API documentation is available through [GitHub Pages](https://kiwiwhale.github.io/kiwi-registry/).

## License

kiwi-registry is licensed under the [Apache License 2.0](LICENSE).
