package io.github.kiwiwhale.kiwiregistry.builder

import io.github.kiwiwhale.kiwiregistry.USER_REGISTRY
import io.github.kiwiwhale.kiwiregistry.User
import io.github.kiwiwhale.kiwiregistry.dsl.registryGraph
import kotlin.test.Test
import kotlin.test.assertEquals

class RegistryEntriesTest {

    @Test
    fun `Entry를 추가한다`() {
        val graph = registryGraph {
            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))
            }
        }

        assertEquals(
            User("Admin", 100),
            graph[USER_REGISTRY]["admin"]
        )
    }

    @Test
    fun `여러 Entry를 추가한다`() {
        val graph = registryGraph {
            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))
                add("guest", User("Guest", 1))
            }
        }

        assertEquals(2, graph[USER_REGISTRY].size)
    }

    @Test
    fun `Entry 크기를 조회한다`() {
        val graph = registryGraph {
            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))
                add("guest", User("Guest", 1))
            }
        }

        assertEquals(2, graph[USER_REGISTRY].size)
    }

    @Test
    fun `빈 Registry를 확인한다`() {
        val graph = registryGraph {
            registry(USER_REGISTRY) {}
        }

        assertEquals(0, graph[USER_REGISTRY].size)
        assertEquals(true, graph[USER_REGISTRY].isEmpty)
    }
}
