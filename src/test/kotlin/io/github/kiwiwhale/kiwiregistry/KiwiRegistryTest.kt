package io.github.kiwiwhale.kiwiregistry

import io.github.kiwiwhale.kiwiregistry.exception.DuplicateRegistryEntryException
import io.github.kiwiwhale.kiwiregistry.exception.RegistryEntryNotFoundException
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KiwiRegistryTest {

    data class TestValue(
        val value: String
    )

    private fun createRegistry() =
        KiwiRegistry<TestValue>("test")

    private fun key(name: String) =
        TestKey(name)

    private fun id(name: String) =
        RegistryId("test", name)

    class TestKey(
        val name: String
    ) : RegistryKey<TestValue> {

        override val id =
            RegistryId("test", name)

        override fun equals(other: Any?): Boolean =
            other is TestKey && id == other.id

        override fun hashCode(): Int =
            id.hashCode()
    }

    @Test
    fun `String으로 등록하고 조회할 수 있다`() {
        val registry = createRegistry()
        val value = TestValue("hello")

        registry.register("user", value)

        assertEquals(value, registry["user"])
    }

    @Test
    fun `RegistryId로 등록하고 조회할 수 있다`() {
        val registry = createRegistry()
        val value = TestValue("hello")

        registry.register(id("user"), value)

        assertEquals(value, registry[id("user")])
    }

    @Test
    fun `Key로 등록하고 조회할 수 있다`() {
        val registry = createRegistry()
        val value = TestValue("hello")

        registry.register(key("user"), value)

        assertEquals(value, registry[key("user")])
    }

    @Test
    fun `서로 다른 접근 방식으로 동일한 항목을 조회할 수 있다`() {
        val registry = createRegistry()
        val value = TestValue("hello")

        registry.register(key("user"), value)

        assertEquals(value, registry["user"])
        assertEquals(value, registry[id("user")])
        assertEquals(value, registry[key("user")])
    }

    @Test
    fun `다른 namespace의 ID를 거부한다`() {
        val registry = createRegistry()

        assertFailsWith<IllegalArgumentException> {
            registry.register(
                RegistryId("other", "user"),
                TestValue("hello")
            )
        }
    }

    @Test
    fun `다른 namespace의 Key를 거부한다`() {
        val registry = createRegistry()

        val otherKey = object : RegistryKey<TestValue> {
            override val id =
                RegistryId("other", "user")
        }

        assertFailsWith<IllegalArgumentException> {
            registry.register(otherKey, TestValue("hello"))
        }
    }

    @Test
    fun `동일한 ID의 중복 등록을 거부한다`() {
        val registry = createRegistry()

        registry.register("user", TestValue("first"))

        assertFailsWith<DuplicateRegistryEntryException> {
            registry.register("user", TestValue("second"))
        }

        assertEquals(
            TestValue("first"),
            registry["user"]
        )
    }

    @Test
    fun `존재하지 않는 항목 조회 시 예외가 발생한다`() {
        val registry = createRegistry()

        assertFailsWith<RegistryEntryNotFoundException> {
            registry["missing"]
        }
    }

    @Test
    fun `존재하지 않는 항목 조회 시 null을 반환한다`() {
        val registry = createRegistry()

        assertNull(registry.getOrNull("missing"))
    }

    @Test
    fun `항목의 존재 여부를 확인할 수 있다`() {
        val registry = createRegistry()

        registry.register("user", TestValue("hello"))

        assertTrue("user" in registry)
        assertTrue(id("user") in registry)
        assertTrue(key("user") in registry)
        assertFalse("missing" in registry)
    }

    @Test
    fun `항목을 등록 해제할 수 있다`() {
        val registry = createRegistry()

        registry.register("user", TestValue("hello"))

        assertTrue(registry.unregister("user"))
        assertFalse("user" in registry)
        assertEquals(0, registry.size)
    }

    @Test
    fun `존재하지 않는 항목의 등록 해제는 false를 반환한다`() {
        val registry = createRegistry()

        assertFalse(registry.unregister("missing"))
    }

    @Test
    fun `여러 항목을 일괄 등록할 수 있다`() {
        val registry = createRegistry()

        registry.registerAllByName(
            mapOf(
                "user" to TestValue("user"),
                "order" to TestValue("order"),
                "payment" to TestValue("payment")
            )
        )

        assertEquals(3, registry.size)
        assertTrue("user" in registry)
        assertTrue("order" in registry)
        assertTrue("payment" in registry)
    }

    @Test
    fun `일괄 등록 실패 시 기존 항목을 유지한다`() {
        val registry = createRegistry()

        registry.register("user", TestValue("original"))

        assertFailsWith<DuplicateRegistryEntryException> {
            registry.registerAllByName(
                mapOf(
                    "order" to TestValue("order"),
                    "user" to TestValue("duplicate")
                )
            )
        }

        assertEquals(1, registry.size)
        assertEquals(TestValue("original"), registry["user"])
        assertFalse("order" in registry)
    }

    @Test
    fun `이미 등록된 항목은 중복 등록하지 않는다`() {
        val registry = createRegistry()

        val first = TestValue("first")
        val second = TestValue("second")

        assertTrue(registry.registerIfAbsent("user", first))
        assertFalse(registry.registerIfAbsent("user", second))

        assertEquals(first, registry["user"])
    }

    @Test
    fun `기존 항목을 교체할 수 있다`() {
        val registry = createRegistry()

        registry.register("user", TestValue("first"))

        assertTrue(
            registry.replace("user", TestValue("second"))
        )

        assertEquals(TestValue("second"), registry["user"])
    }

    @Test
    fun `존재하지 않는 항목은 교체하지 않는다`() {
        val registry = createRegistry()

        assertFalse(
            registry.replace("missing", TestValue("value"))
        )
    }

    @Test
    fun `모든 항목을 제거할 수 있다`() {
        val registry = createRegistry()

        registry.register("user", TestValue("user"))
        registry.register("order", TestValue("order"))

        registry.clear()

        assertEquals(0, registry.size)
        assertTrue(registry.isEmpty())
        assertTrue(registry.ids().isEmpty())
        assertTrue(registry.keys().isEmpty())
    }

    @Test
    fun `빈 Registry의 상태를 확인할 수 있다`() {
        val registry = createRegistry()

        assertTrue(registry.isEmpty())
        assertEquals(0, registry.size)
    }

    @Test
    fun `등록된 ID 목록을 조회할 수 있다`() {
        val registry = createRegistry()

        registry.register("user", TestValue("user"))
        registry.register("order", TestValue("order"))

        assertEquals(
            setOf(id("user"), id("order")),
            registry.ids()
        )
    }

    @Test
    fun `등록된 Key 목록을 조회할 수 있다`() {
        val registry = createRegistry()

        registry.register(key("user"), TestValue("user"))
        registry.register(key("order"), TestValue("order"))

        assertEquals(
            setOf(key("user"), key("order")),
            registry.keys()
        )
    }

    @Test
    fun `Snapshot은 이후 Registry 변경에 영향을 받지 않는다`() {
        val registry = createRegistry()

        registry.register("user", TestValue("user"))

        val snapshot = registry.snapshot()

        registry.register("order", TestValue("order"))
        registry.unregister("user")

        assertEquals(1, snapshot.size)
        assertTrue(snapshot.containsValue(TestValue("user")))
        assertEquals(1, registry.size)
    }

    @Test
    fun `Snapshot을 직접 수정할 수 없다`() {
        val registry = createRegistry()

        registry.register("user", TestValue("user"))

        val snapshot = registry.snapshot()

        @Suppress("UNCHECKED_CAST")
        val mutable =
            snapshot as MutableMap<RegistryKey<TestValue>, TestValue>

        assertFailsWith<UnsupportedOperationException> {
            mutable[key("order")] = TestValue("order")
        }
    }

    @Test
    fun `동시에 같은 ID를 등록해도 하나만 성공한다`() {
        val registry = createRegistry()

        val executor = Executors.newFixedThreadPool(10)
        val start = CountDownLatch(1)
        val successCount = AtomicInteger()

        try {
            val futures = (1..10).map { index ->
                executor.submit(
                    Callable {
                        start.await()

                        if (
                            registry.registerIfAbsent(
                                "user",
                                TestValue("value-$index")
                            )
                        ) {
                            successCount.incrementAndGet()
                        }
                    }
                )
            }

            start.countDown()
            futures.forEach { it.get() }

            assertEquals(1, successCount.get())
            assertEquals(1, registry.size)
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `동시에 서로 다른 ID를 등록해도 데이터가 유실되지 않는다`() {
        val registry = createRegistry()

        val executor = Executors.newFixedThreadPool(10)
        val start = CountDownLatch(1)

        try {
            val futures = (1..100).map { index ->
                executor.submit(
                    Callable {
                        start.await()

                        registry.register(
                            "entry-$index",
                            TestValue("value-$index")
                        )
                    }
                )
            }

            start.countDown()
            futures.forEach { it.get() }

            assertEquals(100, registry.size)
        } finally {
            executor.shutdownNow()
        }
    }
}
