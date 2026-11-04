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
```

Add the dependency:

```kotlin
dependencies {
    implementation("io.github.kiwiwhale:kiwi-registry:1.0.1")
}
```

## Quick Start

### Registry ID

A `RegistryId` identifies a registry using a namespace and key.

```kotlin
val id = RegistryId(
    namespace = "example",
    key = "items"
)

println(id) // example:items
```

You can also create an ID from a string:

```kotlin
val id = RegistryId.fromString("example:items")
```

### Simple Registry Key

Create a simple registry key with `RegistryKey.simple()`:

```kotlin
val key = RegistryKey.simple<String, Int>(
    RegistryId("example", "items")
)
```

### Grouped Registry Key

For grouped entries, use `RegistryKey.grouped()`:

```kotlin
val key = RegistryKey.grouped<String, String, MyGroupedEntry>(
    RegistryId("example", "grouped_items")
)
```

`MyGroupedEntry` must implement `GroupedEntry<Group>`.

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