package io.github.kiwiwhale.kiwiregistry.dsl

/**
 * Restricts implicit receiver access within the registry DSL.
 *
 * Prevents accidental access to outer registry DSL receivers
 * from nested DSL scopes.
 */
@DslMarker
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.TYPE
)
@Retention(AnnotationRetention.BINARY)
annotation class RegistryDslMarker
