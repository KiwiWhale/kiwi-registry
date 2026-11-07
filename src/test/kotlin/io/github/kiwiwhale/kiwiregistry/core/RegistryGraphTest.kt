package io.github.kiwiwhale.kiwiregistry.core

import io.github.kiwiwhale.kiwiregistry.PRODUCT_REGISTRY
import io.github.kiwiwhale.kiwiregistry.USER_REGISTRY
import io.github.kiwiwhale.kiwiregistry.User
import io.github.kiwiwhale.kiwiregistry.dsl.registryGraph
import io.github.kiwiwhale.kiwiregistry.exception.RegistryNotFoundException
import kotlin.test.*

class RegistryGraphTest {

    @Test
    fun `Registry를 조회한다`() {
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
    fun `존재하지 않는 Registry를 조회하면 예외를 발생시킨다`() {
        assertFailsWith<RegistryNotFoundException> {
            RegistryGraph.EMPTY[USER_REGISTRY]
        }
    }

    @Test
    fun `존재하는 Registry를 find로 조회한다`() {
        val graph = registryGraph {
            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))
            }
        }

        assertTrue(graph.find(USER_REGISTRY) != null)
    }

    @Test
    fun `존재하지 않는 Registry의 find 결과는 null이다`() {
        assertNull(RegistryGraph.EMPTY.find(USER_REGISTRY))
    }

    @Test
    fun `Registry 존재 여부를 확인한다`() {
        val graph = registryGraph {
            registry(USER_REGISTRY) {}
        }

        assertTrue(USER_REGISTRY in graph)
        assertTrue(USER_REGISTRY.id in graph)
    }

    @Test
    fun `RegistryGraph의 크기를 조회한다`() {
        val graph = registryGraph {
            registry(USER_REGISTRY) {}
            registry(PRODUCT_REGISTRY) {}
        }

        assertEquals(2, graph.size)
    }

    @Test
    fun `RegistryGraph의 불변성을 유지한다`() {
        val graph = registryGraph {
            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))
            }
        }

        @Suppress("UNCHECKED_CAST")
        val mutable = graph[USER_REGISTRY].data as MutableMap<String, User>

        assertFailsWith<UnsupportedOperationException> {
            mutable["guest"] = User("Guest", 1)
        }
    }

    @Test
    fun `RegistryGraph의 빈 인스턴스를 제공한다`() {
        assertTrue(RegistryGraph.EMPTY.isEmpty)
        assertEquals(0, RegistryGraph.EMPTY.size)
    }
}
