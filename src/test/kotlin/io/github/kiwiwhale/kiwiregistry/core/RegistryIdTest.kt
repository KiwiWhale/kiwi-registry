package io.github.kiwiwhale.kiwiregistry.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RegistryIdTest {

    @Test
    fun `정상적인 RegistryId를 생성한다`() {
        val id = RegistryId("example", "users")

        assertEquals("example", id.namespace)
        assertEquals("users", id.key)
        assertEquals("example:users", id.toString())
    }

    @Test
    fun `빈 namespace를 거부한다`() {
        assertFailsWith<IllegalArgumentException> {
            RegistryId("", "users")
        }
    }

    @Test
    fun `빈 key를 거부한다`() {
        assertFailsWith<IllegalArgumentException> {
            RegistryId("example", "")
        }
    }

    @Test
    fun `콜론이 포함된 namespace를 거부한다`() {
        assertFailsWith<IllegalArgumentException> {
            RegistryId("example:test", "users")
        }
    }

    @Test
    fun `콜론이 포함된 key를 거부한다`() {
        assertFailsWith<IllegalArgumentException> {
            RegistryId("example", "test:users")
        }
    }

    @Test
    fun `문자열을 RegistryId로 파싱한다`() {
        val id = RegistryId.parse("example:users")

        assertEquals(
            RegistryId("example", "users"),
            id
        )
    }

    @Test
    fun `잘못된 문자열 형식을 거부한다`() {
        val invalidIds = listOf(
            "",
            "example",
            ":users",
            "example:",
            "example:users:extra"
        )

        invalidIds.forEach { value ->
            assertFailsWith<IllegalArgumentException> {
                RegistryId.parse(value)
            }
        }
    }

    @Test
    fun `RegistryId는 파싱 후에도 동등성을 유지한다`() {
        val original = RegistryId("example", "users")
        val parsed = RegistryId.parse(original.toString())

        assertEquals(original, parsed)
        assertEquals(original.hashCode(), parsed.hashCode())
    }
}
