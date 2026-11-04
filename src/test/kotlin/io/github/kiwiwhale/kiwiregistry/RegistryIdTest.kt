package io.github.kiwiwhale.kiwiregistry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RegistryIdTest {

    @Test
    fun `namespace와 key로 RegistryId를 생성할 수 있다`() {
        val id = RegistryId("kiwi-flow", "workflow")

        assertEquals("kiwi-flow", id.namespace)
        assertEquals("workflow", id.key)
        assertEquals("kiwi-flow:workflow", id.toString())
    }

    @Test
    fun `문자열에서 RegistryId를 파싱할 수 있다`() {
        val id = RegistryId.parse("kiwi-flow:workflow")

        assertEquals(
            RegistryId("kiwi-flow", "workflow"),
            id
        )
    }

    @Test
    fun `동일한 RegistryId는 동등하다`() {
        val first = RegistryId("kiwi-flow", "workflow")
        val second = RegistryId("kiwi-flow", "workflow")

        assertEquals(first, second)
    }

    @Test
    fun `빈 namespace를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            RegistryId("", "workflow")
        }
    }

    @Test
    fun `빈 key를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            RegistryId("kiwi-flow", "")
        }
    }

    @Test
    fun `namespace에 구분자를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            RegistryId("kiwi:flow", "workflow")
        }
    }

    @Test
    fun `key에 구분자를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            RegistryId("kiwi-flow", "user:workflow")
        }
    }

    @Test
    fun `잘못된 RegistryId 문자열을 거부한다`() {
        val invalidIds = listOf(
            "workflow",
            ":workflow",
            "kiwi-flow:",
            "kiwi:flow:workflow",
            ""
        )

        invalidIds.forEach { value ->
            assertFailsWith<IllegalArgumentException> {
                RegistryId.parse(value)
            }
        }
    }
}