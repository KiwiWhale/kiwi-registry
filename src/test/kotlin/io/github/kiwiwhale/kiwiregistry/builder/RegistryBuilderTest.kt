package io.github.kiwiwhale.kiwiregistry.builder

import io.github.kiwiwhale.kiwiregistry.*
import io.github.kiwiwhale.kiwiregistry.core.registryKey
import io.github.kiwiwhale.kiwiregistry.dsl.registryGraph
import io.github.kiwiwhale.kiwiregistry.exception.RegistryBuildException
import io.github.kiwiwhale.kiwiregistry.exception.RegistryTypeMismatchException
import kotlin.test.*

class RegistryBuilderTest {

    @Test
    fun `Registry를 생성한다`() {
        val graph = registryGraph {
            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))
            }
        }

        assertEquals(1, graph.size)
        assertEquals(
            User("Admin", 100),
            graph[USER_REGISTRY]["admin"]
        )
    }

    @Test
    fun `여러 Registry를 생성한다`() {
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
    fun `여러 Contributor가 동일한 Registry에 기여한다`() {
        val graph = registryGraph {
            contribute(DEFAULT_USERS)
            contribute(DEVELOPER_USERS)
        }

        val users = graph[USER_REGISTRY]

        assertEquals(3, users.size)
        assertTrue("admin" in users)
        assertTrue("guest" in users)
        assertTrue("developer" in users)
    }

    @Test
    fun `중복 Entry를 거부한다`() {
        assertFailsWith<RegistryBuildException> {
            registryGraph {
                registry(USER_REGISTRY) {
                    add("admin", User("Admin", 100))
                    add("admin", User("Another Admin", 200))
                }
            }
        }
    }

    @Test
    fun `중복 Contributor를 거부한다`() {
        assertFailsWith<IllegalStateException> {
            registryGraph {
                contributor("default") {
                    registry(USER_REGISTRY) {
                        add("admin", User("Admin", 100))
                    }
                }

                contributor("default") {
                    registry(USER_REGISTRY) {
                        add("guest", User("Guest", 1))
                    }
                }
            }
        }
    }

    @Test
    fun `Contributor 실패 시 모든 변경 사항을 롤백한다`() {
        val builder = RegistryBuilder()

        builder.contributor("first") {
            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))
            }
        }

        assertFailsWith<IllegalStateException> {
            builder.contributor("failing") {
                registry(USER_REGISTRY) {
                    add("guest", User("Guest", 1))
                }

                error("Contributor failure")
            }
        }

        val graph = builder.build()

        assertEquals(1, graph[USER_REGISTRY].size)
        assertTrue("admin" in graph[USER_REGISTRY])
        assertFalse("guest" in graph[USER_REGISTRY])
    }

    @Test
    fun `Registry 등록 실패 시 기존 Entry를 유지한다`() {
        val builder = RegistryBuilder()

        builder.registry(USER_REGISTRY) {
            add("admin", User("Admin", 100))
        }

        assertFailsWith<RegistryBuildException> {
            builder.registry(USER_REGISTRY) {
                add("guest", User("Guest", 1))
                add("admin", User("Duplicate", 10))
            }
        }

        val graph = builder.build()

        assertEquals(1, graph[USER_REGISTRY].size)
        assertFalse("guest" in graph[USER_REGISTRY])
    }

    @Test
    fun `동일한 ID에 다른 타입의 Registry 등록을 거부한다`() {
        val incompatibleKey = registryKey<Int, User>(
                "example",
                "users"
            )

        assertFailsWith<RegistryTypeMismatchException> {
            registryGraph {
                registry(USER_REGISTRY) {
                    add("admin", User("Admin", 100))
                }

                registry(incompatibleKey) {
                    add(1, User("Other", 1))
                }
            }
        }
    }

    @Test
    fun `Build 이후 Builder 재사용을 거부한다`() {
        val builder = RegistryBuilder()

        builder.build()

        assertFailsWith<IllegalStateException> {
            builder.registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))
            }
        }
    }

    @Test
    fun `Build 이후 중복 Build를 거부한다`() {
        val builder = RegistryBuilder()

        builder.build()

        assertFailsWith<IllegalStateException> {
            builder.build()
        }
    }
}
