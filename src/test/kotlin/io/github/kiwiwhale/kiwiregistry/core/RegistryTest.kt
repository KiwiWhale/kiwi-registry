package io.github.kiwiwhale.kiwiregistry.core

import io.github.kiwiwhale.kiwiregistry.USER_REGISTRY
import io.github.kiwiwhale.kiwiregistry.User
import io.github.kiwiwhale.kiwiregistry.dsl.registryGraph
import io.github.kiwiwhale.kiwiregistry.exception.RegistryEntryNotFoundException
import kotlin.test.*

class RegistryTest {

    private val registry = registryGraph {
        registry(USER_REGISTRY) {
            add("admin", User("Admin", 100))
        }
    }[USER_REGISTRY]

    @Test
    fun `존재하는 Entry를 getOrThrow로 조회한다`() {
        assertEquals(
            User("Admin", 100),
            registry.getOrThrow("admin")
        )
    }

    @Test
    fun `존재하지 않는 Entry 조회 시 예외를 발생시킨다`() {
        assertFailsWith<RegistryEntryNotFoundException> {
            registry.getOrThrow("guest")
        }
    }

    @Test
    fun `값의 존재 여부를 확인한다`() {
        assertTrue(
            registry.containsValue(User("Admin", 100))
        )

        assertFalse(
            registry.containsValue(User("Guest", 1))
        )
    }

    @Test
    fun `getOrDefault는 기본값을 반환한다`() {
        val default = User("Guest", 1)

        assertEquals(
            default,
            registry.getOrDefault("guest", default)
        )
    }
}
