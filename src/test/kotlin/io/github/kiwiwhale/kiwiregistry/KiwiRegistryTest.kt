package io.github.kiwiwhale.kiwiregistry

import io.github.kiwiwhale.kiwiregistry.KiwiRegistry
import io.github.kiwiwhale.kiwiregistry.RegistryId
import io.github.kiwiwhale.kiwiregistry.RegistryKey
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

    data class TestKey(
        override val id: RegistryId
    ) : RegistryKey<TestValue>

    data class TestValue(
        val value: String
    )

    private fun createRegistry() =
        KiwiRegistry<TestValue, TestKey>(
            keyFactory = ::TestKey
        )

    private fun key(name: String) =
        TestKey(RegistryId("test", name))

    private fun id(name: String) =
        RegistryId("test", name)

    @Test
    fun `Key로 등록하고 조회할 수 있다`() {
        val registry = createRegistry()
        val key = key("user")
        val value = TestValue("hello")

        registry.register(key, value)

        assertEquals(value, registry.get(key))
    }

    @Test
    fun `RegistryId로 등록하고 조회할 수 있다`() {
        val registry = createRegistry()
        val value = TestValue("hello")

        registry.register(id("user"), value)

        assertEquals(value, registry.get(id("user")))
    }

    @Test
    fun `Key로 등록한 항목을 RegistryId로 조회할 수 있다`() {
        val registry = createRegistry()
        val value = TestValue("hello")

        registry.register(key("user"), value)

        assertEquals(value, registry.get(id("user")))
    }

    @Test
    fun `RegistryId로 등록한 항목을 Key로 조회할 수 있다`() {
        val registry = createRegistry()
        val value = TestValue("hello")

        registry.register(id("user"), value)

        assertEquals(value, registry.get(key("user")))
    }

    @Test
    fun `동일한 ID의 중복 등록을 거부한다`() {
        val registry = createRegistry()

        registry.register(key("user"), TestValue("first"))

        assertFailsWith<DuplicateRegistryEntryException> {
            registry.register(key("user"), TestValue("second"))
        }

        assertEquals(
            TestValue("first"),
            registry.get(id("user"))
        )
    }

    @Test
    fun `존재하지 않는 항목 조회 시 예외가 발생한다`() {
        val registry = createRegistry()

        assertFailsWith<RegistryEntryNotFoundException> {
            registry.get(id("missing"))
        }
    }

    @Test
    fun `존재하지 않는 항목 조회 시 null을 반환한다`() {
        val registry = createRegistry()

        assertNull(registry.getOrNull(id("missing")))
    }

    @Test
    fun `항목의 존재 여부를 확인할 수 있다`() {
        val registry = createRegistry()

        registry.register(key("user"), TestValue("hello"))

        assertTrue(registry.contains(key("user")))
        assertTrue(registry.contains(id("user")))
        assertFalse(registry.contains(id("missing")))
    }

    @Test
    fun `항목을 등록 해제할 수 있다`() {
        val registry = createRegistry()

        registry.register(key("user"), TestValue("hello"))

        assertTrue(registry.unregister(key("user")))
        assertFalse(registry.contains(id("user")))
        assertEquals(0, registry.size)
    }

    @Test
    fun `존재하지 않는 항목의 등록 해제는 false를 반환한다`() {
        val registry = createRegistry()

        assertFalse(registry.unregister(id("missing")))
    }

    @Test
    fun `여러 항목을 일괄 등록할 수 있다`() {
        val registry = createRegistry()

        registry.registerAll(
            linkedMapOf(
                key("user") to TestValue("user"),
                key("order") to TestValue("order"),
                key("payment") to TestValue("payment")
            )
        )

        assertEquals(3, registry.size)
        assertTrue(registry.contains(id("user")))
        assertTrue(registry.contains(id("order")))
        assertTrue(registry.contains(id("payment")))
    }

    @Test
    fun `일괄 등록 실패 시 기존 항목을 유지한다`() {
        val registry = createRegistry()

        registry.register(key("user"), TestValue("original"))

        assertFailsWith<DuplicateRegistryEntryException> {
            registry.registerAll(
                linkedMapOf(
                    key("order") to TestValue("order"),
                    key("user") to TestValue("duplicate")
                )
            )
        }

        assertEquals(1, registry.size)
        assertEquals(
            TestValue("original"),
            registry.get(id("user"))
        )

        assertFalse(registry.contains(id("order")))
    }

    @Test
    fun `이미 등록된 항목은 중복 등록하지 않는다`() {
        val registry = createRegistry()

        val first = TestValue("first")
        val second = TestValue("second")

        assertTrue(
            registry.registerIfAbsent(key("user"), first)
        )

        assertFalse(
            registry.registerIfAbsent(key("user"), second)
        )

        assertEquals(first, registry.get(id("user")))
    }

    @Test
    fun `RegistryId로 중복 없이 등록할 수 있다`() {
        val registry = createRegistry()

        assertTrue(
            registry.registerIfAbsent(id("user"), TestValue("first"))
        )

        assertFalse(
            registry.registerIfAbsent(id("user"), TestValue("second"))
        )
    }

    @Test
    fun `기존 항목을 교체할 수 있다`() {
        val registry = createRegistry()

        registry.register(key("user"), TestValue("first"))

        assertTrue(
            registry.replace(id("user"), TestValue("second"))
        )

        assertEquals(
            TestValue("second"),
            registry.get(id("user"))
        )
    }

    @Test
    fun `존재하지 않는 항목은 교체하지 않는다`() {
        val registry = createRegistry()

        assertFalse(
            registry.replace(id("missing"), TestValue("value"))
        )
    }

    @Test
    fun `모든 항목을 제거할 수 있다`() {
        val registry = createRegistry()

        registry.register(key("user"), TestValue("user"))
        registry.register(key("order"), TestValue("order"))

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

        registry.register(key("user"), TestValue("user"))
        registry.register(key("order"), TestValue("order"))

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

        registry.register(key("user"), TestValue("user"))

        val snapshot = registry.snapshot()

        registry.register(key("order"), TestValue("order"))
        registry.unregister(id("user"))

        assertEquals(1, snapshot.size)
        assertTrue(snapshot.containsValue(TestValue("user")))
        assertEquals(1, registry.size)
    }

    @Test
    fun `Snapshot을 직접 수정할 수 없다`() {
        val registry = createRegistry()

        registry.register(key("user"), TestValue("user"))

        val snapshot = registry.snapshot()

        @Suppress("UNCHECKED_CAST")
        val mutable = snapshot as MutableMap<TestKey, TestValue>

        assertFailsWith<UnsupportedOperationException> {
            mutable[key("order")] = TestValue("order")
        }
    }

    @Test
    fun `Key 목록을 직접 수정할 수 없다`() {
        val registry = createRegistry()

        registry.register(key("user"), TestValue("user"))

        @Suppress("UNCHECKED_CAST")
        val mutable = registry.keys() as MutableSet<TestKey>

        assertFailsWith<UnsupportedOperationException> {
            mutable.add(key("order"))
        }
    }

    @Test
    fun `서로 다른 namespace는 별개의 항목으로 취급한다`() {
        val registry = createRegistry()

        registry.register(
            TestKey(RegistryId("kiwi-flow", "user")),
            TestValue("flow")
        )

        registry.register(
            TestKey(RegistryId("kiwi-state", "user")),
            TestValue("state")
        )

        assertEquals(2, registry.size)

        assertEquals(
            TestValue("flow"),
            registry.get(RegistryId("kiwi-flow", "user"))
        )

        assertEquals(
            TestValue("state"),
            registry.get(RegistryId("kiwi-state", "user"))
        )
    }

    @Test
    fun `Key Factory가 다른 ID를 반환하면 예외가 발생한다`() {
        val registry = KiwiRegistry<TestValue, TestKey> {
            key("different")
        }

        assertFailsWith<IllegalArgumentException> {
            registry.register(id("user"), TestValue("user"))
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
                                id("user"),
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
                            id("entry-$index"),
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