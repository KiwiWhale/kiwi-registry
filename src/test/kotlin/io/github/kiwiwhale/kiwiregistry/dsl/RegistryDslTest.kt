package io.github.kiwiwhale.kiwiregistry.dsl

import io.github.kiwiwhale.kiwiregistry.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RegistryDslTest {

    @Test
    fun `DSL로 RegistryGraph를 생성한다`() {
        val graph = registryGraph {
            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))
            }
        }

        assertEquals(1, graph.size)
    }

    @Test
    fun `DSL에서 여러 Registry를 생성한다`() {
        val graph = registryGraph {
            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))
            }

            registry(PRODUCT_REGISTRY) {
                add("keyboard", Product("Keyboard", 100))
            }
        }

        assertEquals(2, graph.size)
    }

    @Test
    fun `DSL에서 Contributor를 등록한다`() {
        val graph = registryGraph {
            contributor("custom-users") {
                registry(USER_REGISTRY) {
                    add("developer", User("Developer", 50))
                }
            }
        }

        assertTrue(
            "developer" in graph[USER_REGISTRY]
        )
    }

    @Test
    fun `DSL에서 Contributor 객체를 적용한다`() {
        val graph = registryGraph {
            contribute(DEFAULT_USERS)
            contribute(DEVELOPER_USERS)
        }

        assertEquals(3, graph[USER_REGISTRY].size)
    }
}
