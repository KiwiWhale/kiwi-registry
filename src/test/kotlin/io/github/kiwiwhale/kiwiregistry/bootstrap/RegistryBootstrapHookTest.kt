package io.github.kiwiwhale.kiwiregistry.bootstrap

import io.github.kiwiwhale.kiwiregistry.exception.PostHookExecutionException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RegistryBootstrapHookTest {

    private val key = RegistryBootstrapTestFixture.registryKey

    private class TestHolder : RegistryHolder

    private val contribution =
        RegistryBootstrapTestFixture::contribution

    @Test
    fun `PreHook과 PostHook을 순서대로 실행한다`() {
        val bootstrap = RegistryBootstrap()
        val executed = mutableListOf<String>()

        bootstrap.submit(
            contribution(TestHolder()) {
                pre { executed += "pre" }
                post { executed += "post" }

                registry(key) {
                    add("sword", "Sword")
                }
            }
        )

        bootstrap.build()

        assertEquals(
            listOf("pre", "post"),
            executed
        )
    }

    @Test
    fun `여러 Contribution의 Hook을 등록 순서대로 실행한다`() {
        val bootstrap = RegistryBootstrap()
        val executed = mutableListOf<String>()

        bootstrap.submit(
            contribution(TestHolder()) {
                pre { executed += "first-pre" }
                post { executed += "first-post" }
            }
        )

        bootstrap.submit(
            contribution(TestHolder()) {
                pre { executed += "second-pre" }
                post { executed += "second-post" }
            }
        )

        bootstrap.build()

        assertEquals(
            listOf(
                "first-pre",
                "second-pre",
                "first-post",
                "second-post"
            ),
            executed
        )
    }

    @Test
    fun `PreHook 실패 시 이후 PreHook을 실행하지 않는다`() {
        val bootstrap = RegistryBootstrap()
        val executed = mutableListOf<String>()

        bootstrap.submit(
            contribution(TestHolder()) {
                pre {
                    executed += "first"
                    error("PreHook failed")
                }
            }
        )

        bootstrap.submit(
            contribution(TestHolder()) {
                pre { executed += "second" }
            }
        )

        assertFailsWith<IllegalStateException> {
            bootstrap.build()
        }

        assertEquals(listOf("first"), executed)
    }

    @Test
    fun `PostHook 실패해도 나머지 PostHook을 실행한다`() {
        val bootstrap = RegistryBootstrap()
        val executed = mutableListOf<String>()

        bootstrap.submit(
            contribution(TestHolder()) {
                post {
                    executed += "first"
                    error("First failed")
                }

                post {
                    executed += "second"
                    error("Second failed")
                }

                post {
                    executed += "third"
                }
            }
        )

        val exception = assertFailsWith<PostHookExecutionException> {
            bootstrap.build()
        }

        assertEquals(
            listOf("first", "second", "third"),
            executed
        )

        assertEquals(1, exception.cause!!.suppressed.size)
    }

    @Test
    fun `PostHook 실패 이후에도 Bootstrap은 완료 상태를 유지한다`() {
        val bootstrap = RegistryBootstrap()

        bootstrap.submit(
            contribution(TestHolder()) {
                registry(key) {
                    add("sword", "Sword")
                }

                post {
                    error("PostHook failed")
                }
            }
        )

        assertFailsWith<PostHookExecutionException> {
            bootstrap.build()
        }

        assertEquals(true, bootstrap.isInitialized)
        assertEquals("Sword", bootstrap.getRegistry(key)["sword"])
    }
}
