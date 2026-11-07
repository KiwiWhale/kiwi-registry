package io.github.kiwiwhale.kiwiregistry.provider

import io.github.kiwiwhale.kiwiregistry.PRODUCT_REGISTRY
import io.github.kiwiwhale.kiwiregistry.Product
import io.github.kiwiwhale.kiwiregistry.USER_REGISTRY
import io.github.kiwiwhale.kiwiregistry.User
import io.github.kiwiwhale.kiwiregistry.core.RegistryGraph
import io.github.kiwiwhale.kiwiregistry.core.registryKey
import io.github.kiwiwhale.kiwiregistry.dsl.registryGraph
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*

class RegistryProviderTest {

    @Test
    fun `초기 RegistryGraph를 제공한다`() {
        val provider = RegistryProvider()

        assertSame(RegistryGraph.EMPTY, provider.graph)
        assertTrue(provider.graph.isEmpty)
    }

    @Test
    fun `초기 RegistryGraph를 설정한다`() {
        val graph = registryGraph {
            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))
            }
        }

        val provider = RegistryProvider(graph)

        assertSame(graph, provider.graph)
    }

    @Test
    fun `RegistryGraph를 교체한다`() {
        val first = RegistryGraph.EMPTY

        val second = registryGraph {
            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))
            }
        }

        val provider = RegistryProvider(first)

        val previous = provider.replace(second)

        assertSame(first, previous)
        assertSame(second, provider.graph)
    }

    @Test
    fun `RegistryGraph를 원자적으로 업데이트한다`() {
        val provider = RegistryProvider()

        val updated = registryGraph {
            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))
            }
        }

        val result = provider.update {
            updated
        }

        assertSame(updated, result)
        assertSame(updated, provider.graph)
    }

    @Test
    fun `예상한 RegistryGraph가 일치할 때 교체한다`() {
        val first = RegistryGraph.EMPTY

        val second = registryGraph {
            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))
            }
        }

        val provider = RegistryProvider(first)

        assertTrue(
            provider.compareAndReplace(first, second)
        )

        assertSame(second, provider.graph)
    }

    @Test
    fun `예상한 RegistryGraph가 일치하지 않으면 교체하지 않는다`() {
        val first = RegistryGraph.EMPTY

        val second = registryGraph {
            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))
            }
        }

        val third = registryGraph {
            registry(PRODUCT_REGISTRY) {
                add("keyboard", Product("Keyboard", 100))
            }
        }

        val provider = RegistryProvider(first)

        assertFalse(
            provider.compareAndReplace(second, third)
        )

        assertSame(first, provider.graph)
    }

    @Test
    fun `구조적으로 비슷한 다른 인스턴스는 교체 조건으로 인정하지 않는다`() {
        val first = RegistryGraph.EMPTY
        val anotherEmpty = registryGraph {}

        val provider = RegistryProvider(first)

        assertNotSame(first, anotherEmpty)

        assertFalse(
            provider.compareAndReplace(
                anotherEmpty,
                RegistryGraph.EMPTY
            )
        )

        assertSame(first, provider.graph)
    }

    @Test
    fun `업데이트 함수에서 예외가 발생하면 기존 Graph를 유지한다`() {
        val initial = registryGraph {
            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))
            }
        }

        val provider = RegistryProvider(initial)

        assertFailsWith<IllegalStateException> {
            provider.update {
                error("Update failure")
            }
        }

        assertSame(initial, provider.graph)
    }

    @Test
    fun `업데이트 결과를 반환한다`() {
        val provider = RegistryProvider()

        val updated = registryGraph {
            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))
            }
        }

        val result = provider.update { updated }

        assertSame(updated, result)
        assertSame(result, provider.graph)
    }

    @Test
    fun `여러 스레드에서 동시에 업데이트해도 변경 사항을 유지한다`() {
        val provider = RegistryProvider()

        val threadCount = 8
        val updatesPerThread = 100

        val ready = CountDownLatch(threadCount)
        val start = CountDownLatch(1)

        val executor = Executors.newFixedThreadPool(threadCount)

        try {
            val futures = (0 until threadCount).map {
                executor.submit {
                    ready.countDown()

                    check(start.await(5, SECONDS)) {
                        "Timed out waiting for concurrent updates."
                    }

                    repeat(updatesPerThread) {
                        provider.update { graph ->
                            val current =
                                graph.getOrNull(COUNTER_KEY)
                                    ?.get("count")
                                    ?.value ?: 0

                            counterGraph(current + 1)
                        }
                    }
                }
            }

            assertTrue(
                ready.await(5, SECONDS),
                "Not all worker threads became ready."
            )

            start.countDown()

            futures.forEach {
                it.get(15, SECONDS)
            }

            assertEquals(
                threadCount * updatesPerThread,
                counterValue(provider.graph)
            )
        } finally {
            start.countDown()
            executor.shutdownNow()
            assertTrue(
                executor.awaitTermination(5, SECONDS),
                "Executor did not terminate."
            )
        }
    }

    @Test
    fun `동시 CAS 실패 시 업데이트 함수를 다시 실행한다`() {
        val provider = RegistryProvider(counterGraph(0))

        val ready = CountDownLatch(2)
        val release = CountDownLatch(1)

        val attempts = AtomicInteger(0)

        val executor = Executors.newFixedThreadPool(2)

        try {
            val futures = (0 until 2).map {
                executor.submit {
                    provider.update { graph ->
                        if (attempts.incrementAndGet() <= 2) {
                            ready.countDown()

                            check(release.await(5, SECONDS)) {
                                "Timed out waiting for CAS contention."
                            }
                        }

                        val current = counterValue(graph)

                        counterGraph(current + 1)
                    }
                }
            }

            assertTrue(
                ready.await(5, SECONDS),
                "Both updates must reach the transformation."
            )

            release.countDown()

            futures.forEach {
                it.get(10, SECONDS)
            }

            assertEquals(2, counterValue(provider.graph))

            // 두 스레드가 동일한 초기 Graph를 읽었으므로
            // 한 번 이상의 CAS 재시도가 발생해야 한다.
            assertTrue(attempts.get() >= 3)
        } finally {
            release.countDown()
            executor.shutdownNow()
            assertTrue(executor.awaitTermination(5, SECONDS))
        }
    }

    @Test
    fun `교체는 기존 Graph 참조를 반환한다`() {
        val first = counterGraph(1)
        val second = counterGraph(2)

        val provider = RegistryProvider(first)

        val previous = provider.replace(second)

        assertSame(first, previous)
        assertSame(second, provider.graph)
    }

    @Test
    fun `Graph 조회는 현재 설치된 참조를 반환한다`() {
        val first = counterGraph(1)
        val second = counterGraph(2)

        val provider = RegistryProvider(first)

        assertSame(first, provider.graph)

        provider.replace(second)

        assertSame(second, provider.graph)
    }

    private fun counterValue(graph: RegistryGraph): Int =
        graph.getOrNull(COUNTER_KEY)
            ?.get("count")
            ?.value ?: 0

    private fun counterGraph(value: Int): RegistryGraph =
        registryGraph {
            registry(COUNTER_KEY) {
                add("count", CounterValue(value))
            }
        }

    private companion object {
        val COUNTER_KEY =
            registryKey<String, CounterValue>(
                "test",
                "counter"
            )
    }

    private data class CounterValue(
        val value: Int
    )
}
