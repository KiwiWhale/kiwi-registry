package io.github.kiwiwhale.kiwiregistry.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RegistryIdTest {

    @Test
    fun `라지스트리를 생성`() {
        val id = RegistryId("game", "items")

        assertEquals("game", id.namespace)
        assertEquals("items", id.key)
        assertEquals("game:items", id.toString())
    }

    @Test
    fun `문자열에서 라지스트리를 생성`() {
        val id = RegistryId.parse("game:items")

        assertEquals(id, RegistryId("game", "items"))
    }

    @Test
    fun `잘못된 문자열 테스트`() {
        assertFailsWith<IllegalArgumentException> {
            RegistryId.parse("game")
        }

        assertFailsWith<IllegalArgumentException> {
            RegistryId.parse(":items")
        }

        assertFailsWith<IllegalArgumentException> {
            RegistryId.parse("game:")
        }
    }

    @Test
    fun `빈 네임스페이스와 키 테스트`() {
        assertFailsWith<IllegalArgumentException> {
            RegistryId("", "items")
        }

        assertFailsWith<IllegalArgumentException> {
            RegistryId("game", "")
        }
    }
}
