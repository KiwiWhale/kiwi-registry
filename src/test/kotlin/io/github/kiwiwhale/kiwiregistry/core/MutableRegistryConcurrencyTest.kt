package io.github.kiwiwhale.kiwiregistry.core

import io.github.kiwiwhale.kiwiregistry.RegistryId
import io.github.kiwiwhale.kiwiregistry.exception.DuplicateEntryException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MutableRegistryConcurrencyTest {

    private fun createRegistry() =
        MutableRegistry.create<Int>(
            RegistryId("test", "concurrent")
        )

    private fun runConcurrently(
        workers: Int,
        action: (Int) -> Unit
    ) {
        val executor = Executors.newFixedThreadPool(workers)
        val ready = CountDownLatch(workers)
        val start = CountDownLatch(1)

        try {
            val futures = (0 until workers).map { index ->
                executor.submit {
                    ready.countDown()

                    check(start.await(10, TimeUnit.SECONDS))

                    action(index)
                }
            }

            assertTrue(
                ready.await(10, TimeUnit.SECONDS),
                "Workers did not become ready."
            )

            start.countDown()

            futures.forEach {
                it.get(30, TimeUnit.SECONDS)
            }
        } finally {
            start.countDown()
            executor.shutdownNow()
            executor.awaitTermination(10, TimeUnit.SECONDS)
        }
    }

    @Test
    fun `동시 등록 시 항목이 누락되지 않는다`() {
        val registry = createRegistry()

        val workers = 8
        val entriesPerWorker = 100

        runConcurrently(workers) { worker ->
            repeat(entriesPerWorker) { index ->
                val key = "$worker-$index"
                registry.register(key, index)
            }
        }

        assertEquals(
            workers * entriesPerWorker,
            registry.size
        )
    }

    @Test
    fun `동시 registerIfAbsent 호출 시 하나만 성공한다`() {
        val registry = createRegistry()

        val workers = 16
        val successes = AtomicInteger()

        runConcurrently(workers) {
            if (registry.registerIfAbsent("shared", it)) {
                successes.incrementAndGet()
            }
        }

        assertEquals(1, successes.get())
        assertEquals(1, registry.size)
    }

    @Test
    fun `동일한 키를 동시에 등록하면 하나만 성공한다`() {
        val registry = createRegistry()

        val workers = 16
        val successes = AtomicInteger()
        val duplicates = AtomicInteger()

        runConcurrently(workers) { worker ->
            try {
                registry.register("shared", worker)
                successes.incrementAndGet()
            } catch (_: DuplicateEntryException) {
                duplicates.incrementAndGet()
            }
        }

        assertEquals(1, successes.get())
        assertEquals(workers - 1, duplicates.get())
        assertEquals(1, registry.size)
    }

    @Test
    fun `동시 일괄 등록 시 원자성이 유지된다`() {
        val registry = createRegistry()

        val successes = AtomicInteger()
        val failures = AtomicInteger()

        runConcurrently(2) { worker ->
            try {
                registry.registerAll(
                    linkedMapOf(
                        "shared" to worker,
                        "entry-$worker" to worker
                    )
                )

                successes.incrementAndGet()
            } catch (_: DuplicateEntryException) {
                failures.incrementAndGet()
            }
        }

        assertEquals(1, successes.get())
        assertEquals(1, failures.get())
        assertEquals(2, registry.size)

        assertTrue("shared" in registry)

        val winner = registry["shared"]

        assertTrue("entry-$winner" in registry)
        assertFalse("entry-${1 - winner}" in registry)
    }

    @Test
    fun `동시 교체 작업에서도 레지스트리 일관성이 유지된다`() {
        val registry = createRegistry()

        registry.register("shared", 0)

        runConcurrently(8) { worker ->
            repeat(100) {
                registry.replace("shared", worker)
            }
        }

        assertEquals(1, registry.size)
        assertTrue(registry["shared"] in 0..7)
    }

    @Test
    fun `동시 해제 작업은 한 번만 성공한다`() {
        val registry = createRegistry()

        registry.register("shared", 1)

        val successes = AtomicInteger()

        runConcurrently(16) {
            if (registry.unregister("shared")) {
                successes.incrementAndGet()
            }
        }

        assertEquals(1, successes.get())
        assertTrue(registry.isEmpty)
    }

    @Test
    fun `동시 조회 시 유효한 스냅샷을 반환한다`() {
        val registry = createRegistry()

        repeat(100) {
            registry.register("entry-$it", it)
        }

        runConcurrently(8) {
            repeat(100) {
                val snapshot = registry.snapshot()

                assertEquals(100, snapshot.size)
                assertEquals(100, snapshot.keys.size)
            }
        }
    }

    @Test
    fun `동시 등록과 스냅샷 조회에서도 일관성이 유지된다`() {
        val registry = createRegistry()

        runConcurrently(8) { worker ->
            repeat(100) { index ->
                registry.register(
                    "$worker-$index",
                    index
                )

                val snapshot = registry.snapshot()

                assertEquals(
                    snapshot.size,
                    snapshot.keys.size
                )
            }
        }

        assertEquals(800, registry.size)
    }
}
