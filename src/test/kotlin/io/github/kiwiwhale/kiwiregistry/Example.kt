package io.github.kiwiwhale.kiwiregistry

import io.github.kiwiwhale.kiwiregistry.bootstrap.BootstrapHolder
import io.github.kiwiwhale.kiwiregistry.bootstrap.RegistryHolder
import io.github.kiwiwhale.kiwiregistry.core.RegistryId
import io.github.kiwiwhale.kiwiregistry.core.RegistryKey

val SERVICES = RegistryKey.simple<String, String>(RegistryId("core", "services"))

object AppBootstrap : BootstrapHolder
object CoreHolder : RegistryHolder

fun bootstrap() {
    // Build registries
    val build = AppBootstrap.inject()

    // Register contributions
    CoreHolder.registryBootstrap(AppBootstrap) {
        registry(SERVICES) {
            add("auth", "AuthService")
            add("db", "DatabaseService")
        }
    }

    // Finalize registries
    build()

    val services = AppBootstrap[SERVICES]
    val auth = services["auth"]

    println(auth)
}

// Later...
//val services = AppBootstrap[SERVICES]
//val auth = services["auth"]

fun main() {
    bootstrap()
}