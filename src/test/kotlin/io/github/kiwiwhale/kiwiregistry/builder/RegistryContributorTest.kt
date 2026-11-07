package io.github.kiwiwhale.kiwiregistry.builder

import io.github.kiwiwhale.kiwiregistry.USER_REGISTRY
import io.github.kiwiwhale.kiwiregistry.User
import io.github.kiwiwhale.kiwiregistry.dsl.registryGraph
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RegistryContributorTest {

    @Test
    fun `Contributor를 생성한다`() {
        val contributor = RegistryContributor("users") {
            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))
            }
        }

        assertEquals("users", contributor.id)
    }

    @Test
    fun `빈 Contributor ID를 거부한다`() {
        assertFailsWith<IllegalArgumentException> {
            RegistryContributor("") {}
        }
    }

    @Test
    fun `Contributor를 RegistryBuilder에 적용한다`() {
        val contributor = RegistryContributor("users") {
            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))
            }
        }

        val graph = registryGraph {
            contribute(contributor)
        }

        assertTrue("admin" in graph[USER_REGISTRY])
    }
}
