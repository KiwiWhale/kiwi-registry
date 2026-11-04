package io.github.kiwiwhale.kiwiregistry.core

import io.github.kiwiwhale.kiwiregistry.RegistryId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class RegistrySnapshotTest {

    private fun createRegistry() =
        MutableRegistry.create<String>(
            RegistryId("test", "snapshot")
        )

    @Test
    fun `스냅샷은 원본 데이터를 보존한다`() {
        val registry = createRegistry()

        registry.register("database", "MySQL")

        val snapshot = registry.snapshotData()

        registry.register("cache", "Redis")

        assertEquals(1, snapshot.size)
        assertEquals("MySQL", snapshot["database"])
        assertEquals(null, snapshot["cache"])
    }

    @Test
    fun `스냅샷 맵은 수정할 수 없다`() {
        val registry = createRegistry()

        registry.register("database", "MySQL")

        val snapshot = registry.snapshot()

        assertFailsWith<UnsupportedOperationException> {
            @Suppress("UNCHECKED_CAST")
            (snapshot as MutableMap<String, String>)["cache"] = "Redis"
        }
    }

    @Test
    fun `스냅샷 키는 수정할 수 없다`() {
        val registry = createRegistry()

        registry.register("database", "MySQL")

        val keys = registry.keys()

        assertFailsWith<UnsupportedOperationException> {
            @Suppress("UNCHECKED_CAST")
            (keys as MutableSet<String>).add("cache")
        }
    }

    @Test
    fun `스냅샷 값은 수정할 수 없다`() {
        val registry = createRegistry()

        registry.register("database", "MySQL")

        val values = registry.values()

        assertFailsWith<UnsupportedOperationException> {
            @Suppress("UNCHECKED_CAST")
            (values as MutableCollection<String>).add("Redis")
        }
    }

    @Test
    fun `스냅샷 엔트리는 수정할 수 없다`() {
        val registry = createRegistry()

        registry.register("database", "MySQL")

        val entries = registry.entries()

        assertFailsWith<UnsupportedOperationException> {
            @Suppress("UNCHECKED_CAST")
            (entries as MutableSet<Map.Entry<String, String>>)
                .clear()
        }
    }

    @Test
    fun `스냅샷 엔트리의 값은 교체할 수 없다`() {
        val registry = createRegistry()

        registry.register("database", "MySQL")

        val entry = registry.entries().first()

        assertFailsWith<UnsupportedOperationException> {
            @Suppress("UNCHECKED_CAST")
            (entry as MutableMap.MutableEntry<String, String>)
                .setValue("PostgreSQL")
        }

        assertEquals("MySQL", registry["database"])
    }

    @Test
    fun `서로 다른 스냅샷은 각기 다른 버전의 데이터를 보존한다`() {
        val registry = createRegistry()

        registry.register("database", "MySQL")

        val first = registry.snapshotData()

        registry.replace("database", "PostgreSQL")

        val second = registry.snapshotData()

        assertEquals("MySQL", first["database"])
        assertEquals("PostgreSQL", second["database"])
        assertNotEquals(first["database"], second["database"])
    }

    @Test
    fun `스냅샷은 삽입 순서를 보존한다`() {
        val registry = createRegistry()

        registry.registerAll(
            linkedMapOf(
                "first" to "1",
                "second" to "2",
                "third" to "3"
            )
        )

        assertEquals(
            listOf("first", "second", "third"),
            registry.snapshotData().keys().toList()
        )
    }
}
