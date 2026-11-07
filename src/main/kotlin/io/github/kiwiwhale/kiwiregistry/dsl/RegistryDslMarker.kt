package io.github.kiwiwhale.kiwiregistry.dsl

@DslMarker
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.TYPE
)
@Retention(AnnotationRetention.BINARY)
annotation class RegistryDslMarker
