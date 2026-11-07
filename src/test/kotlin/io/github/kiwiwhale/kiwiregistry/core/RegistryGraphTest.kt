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

    @Test
    fun `여러 RegistryGraph를 병합한다`() {
        val first = registryGraph {
            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))
            }
        }

        val second = registryGraph {
            registry(PRODUCT_REGISTRY) {}
        }

        val merged = RegistryGraph.merge(first, second)

        assertEquals(2, merged.size)
        assertTrue(USER_REGISTRY in merged)
        assertTrue(PRODUCT_REGISTRY in merged)
    }

    @Test
    fun `동일한 Registry의 Entry를 병합한다`() {
        val first = registryGraph {
            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))
                add("guest", User("Guest", 10))
            }
        }

        val second = registryGraph {
            registry(USER_REGISTRY) {
                add("moderator", User("Moderator", 50))
            }
        }

        val merged = RegistryGraph.merge(first, second)

        val registry = merged[USER_REGISTRY]

        assertEquals(3, registry.size)
        assertEquals(
            listOf("admin", "guest", "moderator"),
            registry.keys.toList()
        )
    }

    @Test
    fun `중복 Entry는 나중에 전달된 Graph의 값으로 교체한다`() {
        val first = registryGraph {
            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))
            }
        }

        val second = registryGraph {
            registry(USER_REGISTRY) {
                add("admin", User("Updated Admin", 200))
            }
        }

        val merged = RegistryGraph.merge(first, second)

        assertEquals(
            User("Updated Admin", 200),
            merged[USER_REGISTRY]["admin"]
        )
    }

    @Test
    fun `병합해도 원본 RegistryGraph는 변경되지 않는다`() {
        val first = registryGraph {
            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))
            }
        }

        val second = registryGraph {
            registry(USER_REGISTRY) {
                add("guest", User("Guest", 10))
            }
        }

        RegistryGraph.merge(first, second)

        assertEquals(1, first[USER_REGISTRY].size)
        assertEquals(1, second[USER_REGISTRY].size)

        assertTrue("guest" !in first[USER_REGISTRY])
        assertTrue("admin" !in second[USER_REGISTRY])
    }

    @Test
    fun `인자가 없는 병합은 EMPTY를 반환한다`() {
        assertSame(
            RegistryGraph.EMPTY,
            RegistryGraph.merge()
        )
    }

    @Test
    fun `병합된 RegistryGraph도 불변성을 유지한다`() {
        val first = registryGraph {
            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))
            }
        }

        val merged = RegistryGraph.merge(first)

        @Suppress("UNCHECKED_CAST")
        val mutable = merged[USER_REGISTRY].data as MutableMap<String, User>

        assertFailsWith<UnsupportedOperationException> {
            mutable["guest"] = User("Guest", 10)
        }
    }
}
