package io.github.kiwiwhale.kiwiregistry.builder

import io.github.kiwiwhale.kiwiregistry.USER_REGISTRY
import io.github.kiwiwhale.kiwiregistry.User
import io.github.kiwiwhale.kiwiregistry.dsl.registryGraph
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import io.github.kiwiwhale.kiwiregistry.exception.RegistryBuildException
import io.github.kiwiwhale.kiwiregistry.exception.RegistryEntryNotFoundException

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
        assertTrue(graph[USER_REGISTRY].isEmpty)
    }

    @Test
    fun `여러 Entry를 addAll로 추가한다`() {
        val graph = registryGraph {
            registry(USER_REGISTRY) {
                addAll(
                    linkedMapOf(
                        "admin" to User("Admin", 100),
                        "guest" to User("Guest", 1)
                    )
                )
            }
        }

        assertEquals(2, graph[USER_REGISTRY].size)
        assertEquals(User("Admin", 100), graph[USER_REGISTRY]["admin"])
        assertEquals(User("Guest", 1), graph[USER_REGISTRY]["guest"])
    }

    @Test
    fun `중복 Entry를 addAll로 추가하면 예외가 발생한다`() {
        assertFailsWith<RegistryBuildException> {
            registryGraph {
                registry(USER_REGISTRY) {
                    add("admin", User("Admin", 100))
                    addAll(
                        mapOf(
                            "admin" to User("Another Admin", 200)
                        )
                    )
                }
            }
        }
    }

    @Test
    fun `Entry 존재 여부를 확인한다`() {
        registryGraph {
            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))

                assertTrue(contains("admin"))
                assertFalse(contains("guest"))
            }
        }
    }

    @Test
    fun `Value 존재 여부를 확인한다`() {
        registryGraph {
            registry(USER_REGISTRY) {
                val admin = User("Admin", 100)

                add("admin", admin)

                assertTrue(containsValue(admin))
                assertFalse(containsValue(User("Guest", 1)))
            }
        }
    }

    @Test
    fun `Entry를 제거한다`() {
        val graph = registryGraph {
            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))

                assertTrue(remove("admin"))
                assertFalse(remove("admin"))
            }
        }

        assertTrue(graph[USER_REGISTRY].isEmpty)
    }

    @Test
    fun `기존 Entry를 교체한다`() {
        val graph = registryGraph {
            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))

                assertTrue(
                    replace("admin", User("Updated Admin", 200))
                )
            }
        }

        assertEquals(
            User("Updated Admin", 200),
            graph[USER_REGISTRY]["admin"]
        )
    }

    @Test
    fun `존재하지 않는 Entry는 교체하지 않는다`() {
        registryGraph {
            registry(USER_REGISTRY) {
                assertFalse(
                    replace("guest", User("Guest", 1))
                )

                assertEquals(0, size)
            }
        }
    }

    @Test
    fun `Entry를 조회한다`() {
        registryGraph {
            registry(USER_REGISTRY) {
                val admin = User("Admin", 100)

                add("admin", admin)

                assertEquals(admin, getOrNull("admin"))
                assertNull(getOrNull("guest"))
                assertEquals(admin, getOrThrow("admin"))
            }
        }
    }

    @Test
    fun `존재하지 않는 Entry 조회 시 예외가 발생한다`() {
        assertFailsWith<RegistryEntryNotFoundException> {
            registryGraph {
                registry(USER_REGISTRY) {
                    getOrThrow("admin")
                }
            }
        }
    }

    @Test
    fun `모든 Entry를 제거한다`() {
        val graph = registryGraph {
            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))
                add("guest", User("Guest", 1))

                assertEquals(2, clear())
                assertEquals(0, size)
                assertTrue(isEmpty)
            }
        }

        assertTrue(graph[USER_REGISTRY].isEmpty)
    }

    @Test
    fun `RegistryEntries 블록 종료 후 접근하면 예외를 발생시킨다`() {
        lateinit var entries: RegistryEntries<String, User>

        registryGraph {
            registry(USER_REGISTRY) {
                entries = this
                add("admin", User("Admin", 100))
            }
        }

        assertFailsWith<IllegalStateException> {
            entries.add("guest", User("Guest", 1))
        }
    }

    @Test
    fun `addAll 실패 시 일부 Entry도 추가되지 않는다`() {
        registryGraph {
            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))

                assertFailsWith<RegistryBuildException> {
                    addAll(
                        linkedMapOf(
                            "guest" to User("Guest", 1),
                            "admin" to User("Duplicate", 200)
                        )
                    )
                }

                assertEquals(1, size)
                assertFalse(contains("guest"))
            }
        }
    }

    @Test
    fun `존재하지 않는 Entry 조회 시 전용 예외가 발생한다`() {
        assertFailsWith<RegistryEntryNotFoundException> {
            registryGraph {
                registry(USER_REGISTRY) {
                    getOrThrow("admin")
                }
            }
        }
    }
}
