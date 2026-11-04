package io.github.kiwiwhale.kiwiregistry.core

import io.github.kiwiwhale.kiwiregistry.RegistryId
import io.github.kiwiwhale.kiwiregistry.RegistryKey
import io.github.kiwiwhale.kiwiregistry.exception.DuplicateEntryException
import io.github.kiwiwhale.kiwiregistry.exception.RegistryEntryNotFoundException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MutableRegistryTest {

    private fun createRegistry() =
        MutableRegistry.create<String>(
            RegistryId("test", "services")
        )

    @Test
    fun `빈 레지스트리를 생성한다`() {
        val registry = createRegistry()

        assertEquals(0, registry.size)
        assertTrue(registry.isEmpty)
        assertTrue(registry.keys().isEmpty())
    }

    @Test
    fun `항목을 등록한다`() {
        val registry = createRegistry()

        registry.register("database", "MySQL")

        assertEquals(1, registry.size)
        assertEquals("MySQL", registry["database"])
    }

    @Test
    fun `타입이 지정된 키를 등록한다`() {
        val registry = createRegistry()

        val key = RegistryKey.of<String>(
            "test",
            "database"
        )

        registry.register(key, "MySQL")

        assertEquals("MySQL", registry[key])
    }

    @Test
    fun `다른 네임스페이스의 타입 지정 키를 거부한다`() {
        val registry = createRegistry()

        val key = RegistryKey.of<String>(
            "other",
            "database"
        )

        assertFailsWith<IllegalArgumentException> {
            registry.register(key, "MySQL")
        }
    }

    @Test
    fun `중복된 항목 등록을 거부한다`() {
        val registry = createRegistry()

        registry.register("database", "MySQL")

        assertFailsWith<DuplicateEntryException> {
            registry.register("database", "PostgreSQL")
        }

        assertEquals("MySQL", registry["database"])
        assertEquals(1, registry.size)
    }

    @Test
    fun `여러 항목을 등록한다`() {
        val registry = createRegistry()

        registry.registerAll(
            mapOf(
                "database" to "MySQL",
                "cache" to "Redis",
                "queue" to "RabbitMQ"
            )
        )

        assertEquals(3, registry.size)
        assertEquals("MySQL", registry["database"])
        assertEquals("Redis", registry["cache"])
        assertEquals("RabbitMQ", registry["queue"])
    }

    @Test
    fun `중복 항목이 존재하면 일괄 등록을 원자적으로 처리한다`() {
        val registry = createRegistry()

        registry.register("database", "MySQL")

        assertFailsWith<DuplicateEntryException> {
            registry.registerAll(
                mapOf(
                    "cache" to "Redis",
                    "database" to "PostgreSQL"
                )
            )
        }

        assertEquals(1, registry.size)
        assertFalse("cache" in registry)
        assertEquals("MySQL", registry["database"])
    }

    @Test
    fun `빈 맵의 일괄 등록을 허용한다`() {
        val registry = createRegistry()

        registry.registerAll(emptyMap())

        assertTrue(registry.isEmpty)
    }

    @Test
    fun `항목이 없을 때 등록한다`() {
        val registry = createRegistry()

        assertTrue(
            registry.registerIfAbsent(
                "database",
                "MySQL"
            )
        )

        assertFalse(
            registry.registerIfAbsent(
                "database",
                "PostgreSQL"
            )
        )

        assertEquals("MySQL", registry["database"])
    }

    @Test
    fun `기존 항목을 교체한다`() {
        val registry = createRegistry()

        registry.register("database", "MySQL")

        assertTrue(
            registry.replace(
                "database",
                "PostgreSQL"
            )
        )

        assertEquals("PostgreSQL", registry["database"])
    }

    @Test
    fun `존재하지 않는 항목은 교체하지 않는다`() {
        val registry = createRegistry()

        assertFalse(
            registry.replace(
                "database",
                "MySQL"
            )
        )

        assertTrue(registry.isEmpty)
    }

    @Test
    fun `기존 항목을 해제한다`() {
        val registry = createRegistry()

        registry.register("database", "MySQL")

        assertTrue(registry.unregister("database"))
        assertFalse(registry.unregister("database"))

        assertTrue(registry.isEmpty)
    }

    @Test
    fun `레지스트리를 초기화한다`() {
        val registry = createRegistry()

        registry.registerAll(
            mapOf(
                "database" to "MySQL",
                "cache" to "Redis"
            )
        )

        registry.clear()

        assertTrue(registry.isEmpty)
        assertEquals(0, registry.size)
    }

    @Test
    fun `존재하지 않는 항목 조회 시 null을 반환한다`() {
        val registry = createRegistry()

        assertNull(registry.getOrNull("missing"))
    }

    @Test
    fun `존재하지 않는 항목 조회 시 예외를 발생시킨다`() {
        val registry = createRegistry()

        assertFailsWith<RegistryEntryNotFoundException> {
            registry["missing"]
        }
    }

    @Test
    fun `항목의 포함 여부를 확인한다`() {
        val registry = createRegistry()

        registry.register("database", "MySQL")

        assertTrue("database" in registry)
        assertFalse("cache" in registry)
    }

    @Test
    fun `키와 값 및 항목을 반환한다`() {
        val registry = createRegistry()

        registry.registerAll(
            linkedMapOf(
                "database" to "MySQL",
                "cache" to "Redis"
            )
        )

        assertEquals(
            setOf("database", "cache"),
            registry.keys()
        )

        assertEquals(
            listOf("MySQL", "Redis"),
            registry.values().toList()
        )

        assertEquals(
            mapOf(
                "database" to "MySQL",
                "cache" to "Redis"
            ),
            registry.entries().associate {
                it.key to it.value
            }
        )
    }

    @Test
    fun `값을 순회한다`() {
        val registry = createRegistry()

        registry.registerAll(
            linkedMapOf(
                "database" to "MySQL",
                "cache" to "Redis"
            )
        )

        assertEquals(
            listOf("MySQL", "Redis"),
            registry.toList()
        )
    }

    @Test
    fun `잘못된 키를 거부한다`() {
        val registry = createRegistry()

        val invalidKeys = listOf(
            "",
            " ",
            "database:primary"
        )

        invalidKeys.forEach { key ->
            assertFailsWith<IllegalArgumentException> {
                registry.register(key, "value")
            }
        }
    }

    @Test
    fun `등록 순서를 유지한다`() {
        val registry = createRegistry()

        registry.register("first", "1")
        registry.register("second", "2")
        registry.register("third", "3")

        assertEquals(
            listOf("first", "second", "third"),
            registry.keys().toList()
        )
    }
}
