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

    @Test
    fun `RegistryId를 문자열로 변환한다`() {
        val id = RegistryId("kiwi", "items")

        assertEquals("kiwi:items", id.toString())
    }

    @Test
    fun `문자열에서 RegistryId를 파싱한다`() {
        val id = RegistryId.parse("kiwi:items")

        assertEquals("kiwi", id.namespace)
        assertEquals("items", id.key)
    }

    @Test
    fun `잘못된 RegistryId 형식을 거부한다`() {
        assertFailsWith<IllegalArgumentException> {
            RegistryId.parse("kiwi")
        }

        assertFailsWith<IllegalArgumentException> {
            RegistryId.parse(":items")
        }

        assertFailsWith<IllegalArgumentException> {
            RegistryId.parse("kiwi:")
        }
    }

    @Test
    fun `RegistryId의 namespace와 key가 비어 있으면 안 된다`() {
        assertFailsWith<IllegalArgumentException> {
            RegistryId("", "items")
        }

        assertFailsWith<IllegalArgumentException> {
            RegistryId("kiwi", " ")
        }
    }
}
