package io.github.kiwiwhale.kiwiregistry.provider

import io.github.kiwiwhale.kiwiregistry.PRODUCT_REGISTRY
import io.github.kiwiwhale.kiwiregistry.Product
import io.github.kiwiwhale.kiwiregistry.USER_REGISTRY
import io.github.kiwiwhale.kiwiregistry.User
import io.github.kiwiwhale.kiwiregistry.core.RegistryGraph
import io.github.kiwiwhale.kiwiregistry.dsl.registryGraph
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.test.*

class RegistryProviderTest {

    @Test
    fun `초기 RegistryGraph를 제공한다`() {
        val provider = RegistryProvider()

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

        assertEquals(graph, provider.graph)
    }

    @Test
    fun `RegistryGraph를 교체한다`() {
        val first = RegistryGraphFixture.empty()
        val second = registryGraph {
            registry(USER_REGISTRY) {
                add("admin", User("Admin", 100))
            }
        }

        val provider = RegistryProvider(first)

        val previous = provider.replace(second)

        assertEquals(first, previous)
        assertEquals(second, provider.graph)
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

        assertEquals(updated, result)
        assertEquals(updated, provider.graph)
    }

    @Test
    fun `예상한 RegistryGraph가 일치할 때 교체한다`() {
        val first = RegistryGraphFixture.empty()
        val second = RegistryGraphFixture.empty()

        val provider = RegistryProvider(first)

        assertTrue(
            provider.compareAndReplace(first, second)
        )

        assertEquals(second, provider.graph)
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
    fun `여러 스레드에서 동시에 업데이트해도 변경 사항을 유지한다`() {
        val provider = RegistryProvider()

        val threadCount = 8
        val updatesPerThread = 100

        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)

        repeat(threadCount) { thread ->
            executor.submit {
                try {
                    repeat(updatesPerThread) {
                        provider.update { graph ->
                            graph
                        }
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        latch.await()
        executor.shutdown()

        assertTrue(provider.graph.isEmpty)
    }
}

private object RegistryGraphFixture {
    fun empty() = io.github.kiwiwhale.kiwiregistry.core.RegistryGraph.EMPTY
}
