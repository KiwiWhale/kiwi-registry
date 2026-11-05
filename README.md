# kiwi-registry

![GitHub License](https://img.shields.io/github/license/KiwiWhale/kiwi-registry?style=flat-square) 
![Maven Repository](https://img.shields.io/badge/Maven-Reposilite-blue?style=flat-square)

**A lightweight and extensible registry library for Kotlin.**

kiwi-registry is a lightweight registry library for managing and accessing typed entries in Kotlin applications.

It provides a type-safe API for defining registries and organizing entries, with support for simple and grouped registry structures.

> [!WARNING]
> Early development. APIs and implementation are subject to change.

## Why kiwi-registry?

kiwi-registry aims to provide a simple and flexible way to manage registries in Kotlin applications.

- **Kotlin-first:** Designed with Kotlin's type system and language features.
- **Type-safe:** Define registries using strongly typed registry keys.
- **Flexible:** Support both simple and grouped registry structures.
- **Extensible:** Build custom registry implementations around a consistent API.
- **Lightweight:** Minimal dependencies and a straightforward API.

## Features

- Type-safe registry keys
- Simple registry structures
- Grouped registry structures
- Namespace-based registry identifiers
- Kotlin-first API

## Installation

Add the KiwiWhale Maven repository to your `build.gradle.kts`:

```kotlin
repositories {
    maven("https://repo.mrjimin.xyz/releases")
}

dependencies {
    implementation("io.github.kiwiwhale:kiwi-registry:1.2.1")
}
```

## Quick Start

### Create a Registry

```kotlin
val key = RegistryKey.of<String>(
    "example",
    "services"
)

val registry = MutableRegistry.create(key)

registry.register("database", "MySQL")
registry.register("cache", "Redis")

println(registry["database"])
```

### Freeze and Thaw

```kotlin
val frozen = registry.freeze()
val mutable = frozen.thaw()

mutable.register("queue", "RabbitMQ")
```

### Bootstrap

```kotlin
val bootstrap = Bootstrap.create {
    registry(key) {
        add("database", "MySQL")
        add("cache", "Redis")
    }
}

val services = bootstrap[key]
println(services["database"])
```

`Bootstrap` creates immutable registries, while `MutableRegistry` supports runtime modifications.

## Registry Keys

kiwi-registry provides different registry key types depending on the structure of your data.

| Type                                   | Description                         |
|----------------------------------------|-------------------------------------|
| `RegistryKey<K, V>`                    | Common registry key interface       |
| `SimpleRegistryKey<K, V>`              | Simple registry key implementation  |
| `GroupedRegistryKey<Id, Group, Value>` | Registry key for grouped entries    |
| `RegistryId`                           | Namespace-based registry identifier |

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

## Project Status

kiwi-registry is currently under active development.

APIs and implementation details are subject to change as the project evolves.

## License

kiwi-registry is licensed under the [Apache License 2.0](LICENSE).