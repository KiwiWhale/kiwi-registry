package io.github.kiwiwhale.kiwiregistry.core

import io.github.kiwiwhale.kiwiregistry.bootstrap.RegistryHolder
import io.github.kiwiwhale.kiwiregistry.exception.RegistryNotFoundException
import io.github.kiwiwhale.kiwiregistry.registry.FrozenRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class RegistryGraphTest {

    private val registryKey =
        RegistryKey.simple<String, String>(
            RegistryId("test", "items")
        )

    private class TestHolder : RegistryHolder

    @Test
    fun `Registry를 Graph에 저장한다`() {
        val registry = FrozenRegistry(
            registryKey,
            mapOf("sword" to "Sword"),
            emptyMap()
        )

        val graph = RegistryGraph(
            mapOf(registryKey to registry)
        )

        assertEquals(1, graph.size)
        assertTrue(registryKey in graph)
    }

    @Test
    fun `Registry를 조회한다`() {
        val registry = FrozenRegistry(
            registryKey,
            mapOf("sword" to "Sword"),
            emptyMap()
        )

        val graph = RegistryGraph(
            mapOf(registryKey to registry)
        )

        assertEquals(
            "Sword",
            graph.getRegistry(registryKey)["sword"]
        )
    }

    @Test
    fun `존재하지 않는 Registry 조회 시 실패한다`() {
        val graph = RegistryGraph(emptyMap())

        assertFailsWith<RegistryNotFoundException> {
            graph.getRegistry(registryKey)
        }
    }

    @Test
    fun `존재하지 않는 Registry 검색 시 null을 반환한다`() {
        val graph = RegistryGraph(emptyMap())

        assertEquals(
            null,
            graph.findRegistry(registryKey)
        )
    }

    @Test
    fun `Registry 존재 여부를 확인한다`() {
        val registry = FrozenRegistry(
            registryKey,
            emptyMap(),
            emptyMap()
        )

        val graph = RegistryGraph(
            mapOf(registryKey to registry)
        )

        assertTrue(registryKey in graph)

        val anotherKey =
            RegistryKey.simple<String, String>(
                RegistryId("test", "blocks")
            )

        assertFalse(anotherKey in graph)
    }

    @Test
    fun `모든 Registry를 조회한다`() {
        val registry = FrozenRegistry(
            registryKey,
            mapOf("sword" to "Sword"),
            emptyMap()
        )

        val graph = RegistryGraph(
            mapOf(registryKey to registry)
        )

        assertEquals(1, graph.all().size)
        assertEquals(registry, graph.all().first())
    }

    @Test
    fun `Graph 생성 이후 원본 Map 변경이 영향을 주지 않는다`() {
        val registry = FrozenRegistry(
            registryKey,
            emptyMap(),
            emptyMap()
        )

        val source = linkedMapOf<
                RegistryKey<*, *>,
                Registry<*, *>
                >(
            registryKey to registry
        )

        val graph = RegistryGraph(source)

        source.clear()

        assertEquals(1, graph.size)
        assertTrue(registryKey in graph)
    }

    @Test
    fun `Graph의 Registry 목록을 외부에서 변경할 수 없다`() {
        val registry = FrozenRegistry(
            registryKey,
            emptyMap(),
            emptyMap()
        )

        val graph = RegistryGraph(
            mapOf(registryKey to registry)
        )

        val all = graph.all()

        assertEquals(1, all.size)
    }

    @Test
    fun `서로 다른 Graph는 독립적으로 유지된다`() {
        val firstRegistry = FrozenRegistry(
            registryKey,
            mapOf("sword" to "Sword"),
            emptyMap()
        )

        val secondRegistry = FrozenRegistry(
            registryKey,
            mapOf("sword" to "Diamond Sword"),
            emptyMap()
        )

        val firstGraph = RegistryGraph(
            mapOf(registryKey to firstRegistry)
        )

        val secondGraph = RegistryGraph(
            mapOf(registryKey to secondRegistry)
        )

        assertNotSame(firstGraph, secondGraph)

        assertEquals(
            "Sword",
            firstGraph.getRegistry(registryKey)["sword"]
        )

        assertEquals(
            "Diamond Sword",
            secondGraph.getRegistry(registryKey)["sword"]
        )
    }
}
