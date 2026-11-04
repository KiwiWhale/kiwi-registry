package io.github.kiwiwhale.kiwiregistry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RegistryIdTest {

    @Test
    fun `레지스트리 ID를 생성한다`() {
        val id = RegistryId("core", "services")

        assertEquals("core", id.namespace)
        assertEquals("services", id.key)
        assertEquals("core:services", id.toString())
    }

    @Test
    fun `팩토리를 사용하여 레지스트리 ID를 생성한다`() {
        val id = RegistryId.of("core", "services")

        assertEquals(
            id,
            RegistryId("core", "services")
        )
    }

    @Test
    fun `레지스트리 ID를 파싱한다`() {
        val id = RegistryId.parse("core:services")

        assertEquals("core", id.namespace)
        assertEquals("services", id.key)
    }

    @Test
    fun `파싱한 ID와 직접 생성한 ID는 서로 같다`() {
        assertEquals(
            RegistryId.parse("core:services"),
            RegistryId("core", "services")
        )
    }

    @Test
    fun `빈 네임스페이스를 거부한다`() {
        assertFailsWith<IllegalArgumentException> {
            RegistryId("", "services")
        }

        assertFailsWith<IllegalArgumentException> {
            RegistryId(" ", "services")
        }
    }

    @Test
    fun `빈 키를 거부한다`() {
        assertFailsWith<IllegalArgumentException> {
            RegistryId("core", "")
        }
    }

    @Test
    fun `네임스페이스에 콜론이 포함된 경우 거부한다`() {
        assertFailsWith<IllegalArgumentException> {
            RegistryId("core:test", "services")
        }
    }

    @Test
    fun `키에 콜론이 포함된 경우 거부한다`() {
        assertFailsWith<IllegalArgumentException> {
            RegistryId("core", "service:test")
        }
    }

    @Test
    fun `잘못된 레지스트리 ID 형식을 거부한다`() {
        val invalidIds = listOf(
            "",
            "core",
            ":services",
            "core:",
            "core:services:extra",
            ":",
            "core::services"
        )

        invalidIds.forEach { value ->
            assertFailsWith<IllegalArgumentException> {
                RegistryId.parse(value)
            }
        }
    }
}
