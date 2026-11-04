package io.github.kiwiwhale.kiwiregistry.core

import io.github.kiwiwhale.kiwiregistry.RegistryKey
import io.github.kiwiwhale.kiwiregistry.exception.DuplicateRegistryEntryException
import io.github.kiwiwhale.kiwiregistry.exception.RegistryEntryNotFoundException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConcurrentRegistryTest {

    data class TestKey(
        override val name: String
    ) : RegistryKey

    data class TestValue(
        val value: String
    )

    private fun createRegistry() =
        ConcurrentRegistry<TestValue, TestKey>(
            keyFactory = ::TestKey
        )

    @Test
    fun `Key로 등록하고 조회할 수 있다`() {
        val registry = createRegistry()
        val key = TestKey("test")
        val value = TestValue("hello")

        registry.register(key, value)

        assertEquals(value, registry.get(key))
    }

    @Test
    fun `문자열로 등록하고 조회할 수 있다`() {
        val registry = createRegistry()
        val value = TestValue("hello")

        registry.register("test", value)

        assertEquals(value, registry.get("test"))
    }

    @Test
    fun `Key로 등록한 항목을 문자열로 조회할 수 있다`() {
        val registry = createRegistry()
        val value = TestValue("hello")

        registry.register(TestKey("test"), value)

        assertEquals(value, registry.get("test"))
    }

    @Test
    fun `중복 등록을 거부한다`() {
        val registry = createRegistry()

        registry.register("test", TestValue("first"))

        assertFailsWith<DuplicateRegistryEntryException> {
            registry.register("test", TestValue("second"))
        }

        assertEquals(
            TestValue("first"),
            registry.get("test")
        )
    }

    @Test
    fun `존재하지 않는 항목 조회 시 예외가 발생한다`() {
        val registry = createRegistry()

        assertFailsWith<RegistryEntryNotFoundException> {
            registry.get("missing")
        }
    }

    @Test
    fun `존재하지 않는 항목 조회 시 null을 반환한다`() {
        val registry = createRegistry()

        assertNull(registry.getOrNull("missing"))
    }

    @Test
    fun `항목을 등록 해제할 수 있다`() {
        val registry = createRegistry()

        registry.register("test", TestValue("hello"))

        assertTrue(registry.unregister("test"))
        assertFalse(registry.contains("test"))
        assertEquals(0, registry.size)
    }

    @Test
    fun `여러 항목을 일괄 등록할 수 있다`() {
        val registry = createRegistry()

        registry.registerAll(
            linkedMapOf(
                TestKey("one") to TestValue("1"),
                TestKey("two") to TestValue("2")
            )
        )

        assertEquals(2, registry.size)
    }

    @Test
    fun `일괄 등록 실패 시 기존 항목을 유지한다`() {
        val registry = createRegistry()

        registry.register("one", TestValue("1"))

        assertFailsWith<DuplicateRegistryEntryException> {
            registry.registerAll(
                linkedMapOf(
                    TestKey("two") to TestValue("2"),
                    TestKey("one") to TestValue("duplicate")
                )
            )
        }

        assertEquals(1, registry.size)
        assertNull(registry.getOrNull("two"))
    }

    @Test
    fun `Snapshot은 이후 변경에 영향을 받지 않는다`() {
        val registry = createRegistry()

        registry.register("one", TestValue("1"))

        val snapshot = registry.snapshot()

        registry.register("two", TestValue("2"))

        assertEquals(1, snapshot.size)
        assertEquals(2, registry.size)
    }

    @Test
    fun `등록된 이름을 조회할 수 있다`() {
        val registry = createRegistry()

        registry.register("one", TestValue("1"))
        registry.register("two", TestValue("2"))

        assertEquals(
            setOf("one", "two"),
            registry.names()
        )
    }
}