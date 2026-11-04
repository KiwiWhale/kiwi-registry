package io.github.kiwiwhale.kiwiregistry.core

import io.github.kiwiwhale.kiwiregistry.exception.RegistryNotFoundException
import io.github.kiwiwhale.kiwiregistry.registry.MutableRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class RegistryGraphTest {

    private val key = RegistryKey.simple<String, String>(
        RegistryId("test", "items")
    )

    @Test
    fun `레지스트리 조회`() {
        val registry = MutableRegistry(key)

        val graph = RegistryGraph(
            mapOf(key to registry)
        )

        assertEquals(registry, graph.getRegistry(key))
    }

    @Test
    fun `존재하지 않는 레지스트리 조회 테스트`() {
        val graph = RegistryGraph(emptyMap())

        assertFailsWith<RegistryNotFoundException> {
            graph.getRegistry(key)
        }
    }

    @Test
    fun `레지스트리를 추가한 새로운 그래프 생성`() {
        val registry = MutableRegistry(key)

        val original = RegistryGraph(emptyMap())
        val updated = original.rebuild(registry)

        assertNotSame(original, updated)
        assertEquals(0, original.size)
        assertEquals(1, updated.size)
        assertTrue(updated.contains(key))
    }

    @Test
    fun `여러 레지스트리를 한 번에 추가`() {
        val firstKey = RegistryKey.simple<String, String>(
            RegistryId("test", "first")
        )

        val secondKey = RegistryKey.simple<String, String>(
            RegistryId("test", "second")
        )

        val first = MutableRegistry(firstKey)
        val second = MutableRegistry(secondKey)

        val graph = RegistryGraph(emptyMap())
            .rebuild(listOf(first, second))

        assertEquals(2, graph.size)
        assertTrue(graph.contains(firstKey))
        assertTrue(graph.contains(secondKey))
    }
}
