package io.github.kiwiwhale.kiwiregistry.builder

import io.github.kiwiwhale.kiwiregistry.*
import io.github.kiwiwhale.kiwiregistry.core.registryKey
import io.github.kiwiwhale.kiwiregistry.dsl.registryGraph
import io.github.kiwiwhale.kiwiregistry.exception.RegistryBuildException
import io.github.kiwiwhale.kiwiregistry.exception.RegistryTypeMismatchException
import io.github.kiwiwhale.kiwiregistry.exception.RegistryValidationException
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
        assertEquals(User("Admin", 100), graph[USER_REGISTRY]["admin"])
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
    fun `실패한 Contributor ID를 다시 사용할 수 있다`() {
        val builder = RegistryBuilder()

        assertFailsWith<IllegalStateException> {
            builder.contributor("retryable") {
                error("Failure")
            }
        }

        builder.contributor("retryable") {
            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))
            }
        }

        assertTrue(builder.hasContributor("retryable"))
        assertEquals(1, builder.contributorCount)
    }

    @Test
    fun `Contributor 실행 순서를 유지한다`() {
        val builder = RegistryBuilder()

        builder.contributor("first") {}
        builder.contributor("second") {}
        builder.contributor("third") {}

        assertEquals(
            listOf("first", "second", "third"),
            builder.contributorIds.toList()
        )
    }

    @Test
    fun `Contributor ID 목록은 수정할 수 없다`() {
        val builder = RegistryBuilder()

        builder.contributor("first") {}

        val ids = builder.contributorIds

        @Suppress("UNCHECKED_CAST")
        val mutable = ids as MutableSet<String>

        assertFailsWith<UnsupportedOperationException> {
            mutable.add("second")
        }
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
    fun `Registry 블록에서 임의 예외 발생 시 기존 Entry를 유지한다`() {
        val builder = RegistryBuilder()

        builder.registry(USER_REGISTRY) {
            add("admin", User("Admin", 100))
        }

        assertFailsWith<IllegalStateException> {
            builder.registry(USER_REGISTRY) {
                add("guest", User("Guest", 10))
                error("Registry configuration failure")
            }
        }

        val graph = builder.build()

        assertEquals(1, graph[USER_REGISTRY].size)
        assertTrue("admin" in graph[USER_REGISTRY])
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
    fun `Registry 존재 여부를 확인한다`() {
        val builder = RegistryBuilder()

        builder.registry(USER_REGISTRY) {}

        assertTrue(builder.containsRegistry(USER_REGISTRY.id))
        assertTrue(builder.containsRegistry(USER_REGISTRY))
        assertFalse(builder.containsRegistry(PRODUCT_REGISTRY.id))
    }

    @Test
    fun `Registry Entry 개수를 조회한다`() {
        val builder = RegistryBuilder()

        builder.registry(USER_REGISTRY) {
            add("admin", User("Admin", 100))
            add("guest", User("Guest", 10))
        }

        assertEquals(2, builder.getEntryCount(USER_REGISTRY.id))
        assertEquals(2, builder.getEntryCount(USER_REGISTRY))
        assertNull(builder.getEntryCount(PRODUCT_REGISTRY.id))
    }

    @Test
    fun `다른 타입의 Registry Entry 개수 조회를 거부한다`() {
        val builder = RegistryBuilder()

        builder.registry(USER_REGISTRY) {}

        val incompatibleKey =
            registryKey<Int, User>(USER_REGISTRY.id)

        assertFailsWith<RegistryTypeMismatchException> {
            builder.getEntryCount(incompatibleKey)
        }
    }

    @Test
    fun `Registry를 제거한다`() {
        val builder = RegistryBuilder()

        builder.registry(USER_REGISTRY) {
            add("admin", User("Admin", 100))
        }

        assertTrue(builder.removeRegistry(USER_REGISTRY))
        assertFalse(builder.containsRegistry(USER_REGISTRY))
        assertEquals(0, builder.registryCount)
    }

    @Test
    fun `존재하지 않는 Registry 제거는 false를 반환한다`() {
        val builder = RegistryBuilder()

        assertFalse(builder.removeRegistry(USER_REGISTRY))
    }

    @Test
    fun `다른 타입의 Registry 제거를 거부한다`() {
        val builder = RegistryBuilder()

        builder.registry(USER_REGISTRY) {}

        val incompatibleKey =
            registryKey<Int, User>(USER_REGISTRY.id)

        assertFailsWith<RegistryTypeMismatchException> {
            builder.removeRegistry(incompatibleKey)
        }

        assertTrue(builder.containsRegistry(USER_REGISTRY))
    }

    @Test
    fun `Registry Entry를 초기화한다`() {
        val builder = RegistryBuilder()

        builder.registry(USER_REGISTRY) {
            add("admin", User("Admin", 100))
            add("guest", User("Guest", 10))
        }

        assertEquals(2, builder.clearRegistry(USER_REGISTRY))

        assertTrue(builder.containsRegistry(USER_REGISTRY))
        assertEquals(0, builder.getEntryCount(USER_REGISTRY))
    }

    @Test
    fun `Entry가 없는 Registry 초기화는 0을 반환한다`() {
        val builder = RegistryBuilder()

        builder.registry(USER_REGISTRY) {}

        assertEquals(0, builder.clearRegistry(USER_REGISTRY))
    }

    @Test
    fun `존재하지 않는 Registry 초기화는 null을 반환한다`() {
        val builder = RegistryBuilder()

        assertNull(builder.clearRegistry(USER_REGISTRY))
    }

    @Test
    fun `Registry 블록 내부에서 Builder 작업을 거부한다`() {
        val builder = RegistryBuilder()

        builder.registry(USER_REGISTRY) {
            assertFailsWith<IllegalStateException> {
                builder.clearRegistry(USER_REGISTRY)
            }

            add("admin", User("Admin", 100))
        }

        assertEquals(
            1,
            builder.build()[USER_REGISTRY].size
        )
    }

    @Test
    fun `Registry 블록의 중첩 실행을 거부한다`() {
        val builder = RegistryBuilder()

        builder.registry(USER_REGISTRY) {
            assertFailsWith<IllegalStateException> {
                builder.registry(PRODUCT_REGISTRY) {}
            }

            add("admin", User("Admin", 100))
        }

        val graph = builder.build()

        assertTrue(USER_REGISTRY in graph)
        assertFalse(PRODUCT_REGISTRY in graph)
    }

    @Test
    fun `Contributor 중첩 실행을 거부한다`() {
        val builder = RegistryBuilder()

        builder.contributor("outer") {
            assertFailsWith<IllegalStateException> {
                contributor("inner") {}
            }

            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))
            }
        }

        assertEquals(
            listOf("outer"),
            builder.contributorIds.toList()
        )
    }

    @Test
    fun `잘못된 Contributor ID를 거부한다`() {
        val builder = RegistryBuilder()

        assertFailsWith<IllegalArgumentException> {
            builder.contributor(" ") {}
        }

        assertFailsWith<IllegalArgumentException> {
            builder.contributor(" padded ") {}
        }
    }

    @Test
    fun `Build 이후 Builder 재사용을 거부한다`() {
        val builder = RegistryBuilder()

        builder.build()

        assertTrue(builder.isBuilt)

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

    @Test
    fun `Build 이후 Contributor 실행을 거부한다`() {
        val builder = RegistryBuilder()

        builder.build()

        assertFailsWith<IllegalStateException> {
            builder.contributor("late") {}
        }
    }

    @Test
    fun `Build 이후 Registry 제거를 거부한다`() {
        val builder = RegistryBuilder()

        builder.build()

        assertFailsWith<IllegalStateException> {
            builder.removeRegistry(USER_REGISTRY)
        }
    }

    @Test
    fun `Build 이후 Registry 초기화를 거부한다`() {
        val builder = RegistryBuilder()

        builder.build()

        assertFailsWith<IllegalStateException> {
            builder.clearRegistry(USER_REGISTRY)
        }
    }


    @Test
    fun `Warning만 존재하면 RegistryGraph를 생성한다`() {
        val graph = registryGraph {
            validate {
                warning("Optional registry is missing.")
            }
        }

        assertTrue(graph.isEmpty)
    }

    @Test
    fun `Info만 존재하면 RegistryGraph를 생성한다`() {
        val graph = registryGraph {
            validate {
                info("Registry validation completed.")
            }
        }

        assertTrue(graph.isEmpty)
    }

    @Test
    fun `Warning과 Info가 함께 존재해도 RegistryGraph를 생성한다`() {
        val graph = registryGraph {
            validate {
                warning("Optional configuration is missing.")
                info("Validation completed.")
            }
        }

        assertTrue(graph.isEmpty)
    }

    @Test
    fun `Error가 존재하면 RegistryGraph 생성을 거부한다`() {
        val builder = RegistryBuilder()

        builder.validate {
            error("Required registry is missing.")
        }

        assertFailsWith<RegistryValidationException> {
            builder.build()
        }

        assertFalse(builder.isBuilt)
    }

    @Test
    fun `Validation 실패 이후 Builder를 수정하고 다시 Build할 수 있다`() {
        val builder = RegistryBuilder()

        builder.validate {
            requireRegistry(USER_REGISTRY)
        }

        assertFailsWith<RegistryValidationException> {
            builder.build()
        }

        builder.registry(USER_REGISTRY) {
            add("admin", User("Admin", 100))
        }

        val graph = builder.build()

        assertEquals(1, graph[USER_REGISTRY].size)
        assertTrue(builder.isBuilt)
    }

    @Test
    fun `Warning이 있는 검증 결과도 빌드 과정에서 유지한다`() {
        val builder = RegistryBuilder()

        builder.validate {
            warning("Optional registry is missing.")
            info("Configuration loaded.")
        }

        val graph = builder.build()

        assertTrue(graph.isEmpty)
        assertTrue(builder.isBuilt)
    }

}
