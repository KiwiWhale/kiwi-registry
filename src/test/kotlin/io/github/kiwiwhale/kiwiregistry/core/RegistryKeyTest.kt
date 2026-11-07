package io.github.kiwiwhale.kiwiregistry.core

import io.github.kiwiwhale.kiwiregistry.User
import kotlin.test.*

class RegistryKeyTest {

    @Test
    fun `RegistryKey를 생성한다`() {
        val key = registryKey<String, User>(
            "example",
            "users"
        )

        assertEquals(
            RegistryId("example", "users"),
            key.id
        )
    }

    @Test
    fun `RegistryId로 RegistryKey를 생성한다`() {
        val id = RegistryId("example", "users")

        val key = registryKey<String, User>(id)

        assertEquals(id, key.id)
    }

    @Test
    fun `동일한 ID를 가진 RegistryKey는 동등하다`() {
        val first = registryKey<String, User>(
            "example",
            "users"
        )

        val second = registryKey<String, User>(
            "example",
            "users"
        )

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `서로 다른 ID를 가진 RegistryKey는 동등하지 않다`() {
        val first = registryKey<String, User>(
            "example",
            "users"
        )

        val second = registryKey<String, User>(
            "example",
            "products"
        )

        assertNotEquals(first, second)
    }

    @Test
    fun `동일한 타입 정보를 가진 RegistryKey를 비교한다`() {
        val first = registryKey<String, User>(
            "example",
            "users"
        )

        val second = registryKey<String, User>(
            "example",
            "users"
        )

        assertTrue(first.hasSameTypes(second))
    }

    @Test
    fun `서로 다른 타입 정보를 구분한다`() {
        val first = registryKey<String, User>(
            "example",
            "users"
        )

        val second = registryKey<Int, User>(
            "example",
            "users"
        )

        assertFalse(first.hasSameTypes(second))
    }
}
