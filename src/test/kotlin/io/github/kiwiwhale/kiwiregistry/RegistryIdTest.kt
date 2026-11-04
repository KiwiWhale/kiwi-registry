package io.github.kiwiwhale.kiwiregistry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RegistryIdTest {

    @Test
    fun `유효한 ID를 생성한다`() {
        val id = RegistryId("minecraft", "stone")

        assertEquals("minecraft", id.namespace)
        assertEquals("stone", id.key)
        assertEquals("minecraft:stone", id.toString())
    }

    @Test
    fun `문자열에서 ID를 파싱한다`() {
        assertEquals(
            RegistryId("minecraft", "stone"),
            RegistryId.parse("minecraft:stone")
        )
    }

    @Test
    fun `잘못된 ID를 거부한다`() {
        listOf(
            "",
            ":stone",
            "minecraft:",
            "minecraft:stone:block",
            "minecraft"
        ).forEach { value ->
            assertFailsWith<IllegalArgumentException> {
                RegistryId.parse(value)
            }
        }
    }

    @Test
    fun `잘못된 namespace를 거부한다`() {
        listOf("", " ", "minecraft:other").forEach {
            assertFailsWith<IllegalArgumentException> {
                RegistryId(it, "stone")
            }
        }
    }

    @Test
    fun `잘못된 key를 거부한다`() {
        listOf("", " ", "stone:block").forEach {
            assertFailsWith<IllegalArgumentException> {
                RegistryId("minecraft", it)
            }
        }
    }
}