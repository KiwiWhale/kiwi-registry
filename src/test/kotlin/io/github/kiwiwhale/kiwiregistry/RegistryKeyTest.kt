package io.github.kiwiwhale.kiwiregistry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class RegistryKeyTest {

    @Test
    fun `타입 지정 레지스트리 키를 생성한다`() {
        val key = RegistryKey.of<String>(
            "core",
            "services"
        )

        assertEquals(
            RegistryId("core", "services"),
            key.id
        )
    }

    @Test
    fun `ID로 레지스트리 키를 생성한다`() {
        val id = RegistryId("core", "services")

        val key = RegistryKey.of<String>(
            id,
            String::class
        )

        assertEquals(id, key.id)
    }

    @Test
    fun `동일한 ID와 타입을 가진 레지스트리 키는 서로 같다`() {
        val first = RegistryKey.of<String>(
            "core",
            "services"
        )

        val second = RegistryKey.of<String>(
            "core",
            "services"
        )

        assertEquals(first, second)
    }

    @Test
    fun `서로 다른 타입을 가진 레지스트리 키는 서로 다르다`() {
        val stringKey = RegistryKey.of<String>(
            "core",
            "services"
        )

        val intKey = RegistryKey.of<Int>(
            "core",
            "services"
        )

        assertNotEquals(
            stringKey,
            intKey as Any
        )
    }

    @Test
    fun `서로 다른 ID를 가진 레지스트리 키는 서로 다르다`() {
        val first = RegistryKey.of<String>(
            "core",
            "services"
        )

        val second = RegistryKey.of<String>(
            "core",
            "commands"
        )

        assertNotEquals(first, second)
    }
}
