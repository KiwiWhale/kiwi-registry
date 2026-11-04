package io.github.kiwiwhale.kiwiregistry.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RegistryKeyTest {

    @Test
    fun `심플 레지스트리 생성`() {
        val id = RegistryId("game", "items")

        val key = RegistryKey.simple<String, String>(id)

        val simpleKey = SimpleRegistryKey<String, String>(id)

        assertEquals(id, key.id)
        assertIs<SimpleRegistryKey<String, String>>(key)

        assertEquals(id, simpleKey.id)
        assertIs<SimpleRegistryKey<String, String>>(simpleKey)
    }

    @Test
    fun `같은 id를 가진 키는 동일하다`() {
        val first = RegistryKey.simple<String, String>(
            RegistryId("game", "items")
        )

        val second = SimpleRegistryKey<String, String>(
            RegistryId("game", "items")
        )

        assertEquals(first, second)
    }
}
