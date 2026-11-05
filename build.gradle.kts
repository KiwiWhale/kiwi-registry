plugins {
    kotlin("jvm") version "2.4.21"
    id("co.uzzu.dotenv.gradle") version "4.0.0"
    `maven-publish`
//    id("org.jetbrains.dokka") version "2.2.0"
}

group = "io.github.kiwiwhale.kiwiregistry"
version = "1.2.2-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(25)
}

tasks.test {
    useJUnitPlatform()
}

//dokka {
//    dokkaPublications.html {
//        moduleName.set("KiwiRegistry")
//
//        pluginsConfiguration.html {
//            footerMessage.set("(c) KiwiWhale")
//        }
//    }
//}

val mavenUsername = if (env.isPresent("MAVEN_USERNAME")) env.fetch("MAVEN_USERNAME") else ""
val mavenPassword = if (env.isPresent("MAVEN_PASSWORD")) env.fetch("MAVEN_PASSWORD") else ""

publishing {
    repositories {
        maven {
            name = "mrjiminRepository"

            url = uri(
                if (version.toString().endsWith("-SNAPSHOT"))
                    "https://repo.mrjimin.xyz/snapshots"
                else
                    "https://repo.mrjimin.xyz/releases"
            )

            credentials {
                username = mavenUsername
                password = mavenPassword
            }

            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            groupId = "io.github.kiwiwhale"
            artifactId = "kiwi-registry"
            version = project.version.toString()

            from(components["java"])
        }
    }
}
