package io.github.kiwiwhale.kiwiregistry.bootstrap

import io.github.kiwiwhale.kiwiregistry.exception.InvalidContributionException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RegistryBootstrapRebuildTest {

    private val key = RegistryBootstrapTestFixture.registryKey

    private val contribution =
        RegistryBootstrapTestFixture::contribution

    private class TestHolder : RegistryHolder

    @Test
    fun `Holder의 Registry를 재구축한다`() {
        val bootstrap = RegistryBootstrap()
        val holder = TestHolder()

        var value = "Sword"

        bootstrap.submit(
            contribution(holder) {
                registry(key) {
                    add("weapon", value)
                }
            }
        )

        bootstrap.build()

        value = "Diamond Sword"

        bootstrap.rebuildRegistries(holder)

        assertEquals(
            "Diamond Sword",
            bootstrap.getRegistry(key)["weapon"]
        )
    }

    @Test
    fun `특정 Holder만 재구축한다`() {
        val bootstrap = RegistryBootstrap()

        val firstHolder = TestHolder()
        val secondHolder = TestHolder()

        var firstValue = "Sword"
        var secondBuildCount = 0

        bootstrap.submit(
            contribution(firstHolder) {
                registry(key) {
                    add("weapon", firstValue)
                }
            }
        )

        bootstrap.submit(
            contribution(secondHolder) {
                registry(key) {
                    secondBuildCount++
                    add("shield", "Shield")
                }
            }
        )

        bootstrap.build()

        firstValue = "Diamond Sword"

        bootstrap.rebuildRegistries(firstHolder)

        val registry = bootstrap.getRegistry(key)

        assertEquals("Diamond Sword", registry["weapon"])
        assertEquals("Shield", registry["shield"])
        assertEquals(1, secondBuildCount)
    }

    @Test
    fun `재구축 이후 다른 Holder의 데이터가 유지된다`() {
        val bootstrap = RegistryBootstrap()

        val firstHolder = TestHolder()
        val secondHolder = TestHolder()

        var firstValue = "Sword"

        bootstrap.submit(
            contribution(firstHolder) {
                registry(key) {
                    add("weapon", firstValue)
                }
            }
        )

        bootstrap.submit(
            contribution(secondHolder) {
                registry(key) {
                    add("shield", "Shield")
                }
            }
        )

        bootstrap.build()

        firstValue = "Diamond Sword"

        bootstrap.rebuildRegistries(firstHolder)

        val registry = bootstrap.getRegistry(key)

        assertEquals(2, registry.size)
        assertEquals("Diamond Sword", registry["weapon"])
        assertEquals("Shield", registry["shield"])
    }

    @Test
    fun `존재하지 않는 Holder를 재구축하면 실패한다`() {
        val bootstrap = RegistryBootstrap()

        bootstrap.build()

        assertFailsWith<InvalidContributionException> {
            bootstrap.rebuildRegistries(TestHolder())
        }
    }

    @Test
    fun `재구축에 실패하면 기존 Registry를 유지한다`() {
        val bootstrap = RegistryBootstrap()
        val holder = TestHolder()

        var shouldFail = false

        bootstrap.submit(
            contribution(holder) {
                registry(key) {
                    add("weapon", "Sword")

                    if (shouldFail) {
                        add("weapon", "Duplicate")
                    }
                }
            }
        )

        bootstrap.build()

        val original = bootstrap.getRegistry(key)

        shouldFail = true

        assertFailsWith<Exception> {
            bootstrap.rebuildRegistries(holder)
        }

        val current = bootstrap.getRegistry(key)

        assertEquals(original.data, current.data)
        assertEquals("Sword", current["weapon"])
        assertTrue(bootstrap.isInitialized)
    }

    @Test
    fun `재구축 시 대상 Holder의 Hook만 실행한다`() {
        val bootstrap = RegistryBootstrap()

        val firstHolder = TestHolder()
        val secondHolder = TestHolder()

        val executed = mutableListOf<String>()

        bootstrap.submit(
            contribution(firstHolder) {
                pre { executed += "first-pre" }
                post { executed += "first-post" }

                registry(key) {
                    add("weapon", "Sword")
                }
            }
        )

        bootstrap.submit(
            contribution(secondHolder) {
                pre { executed += "second-pre" }
                post { executed += "second-post" }

                registry(key) {
                    add("shield", "Shield")
                }
            }
        )

        bootstrap.build()

        executed.clear()

        bootstrap.rebuildRegistries(firstHolder)

        assertEquals(
            listOf("first-pre", "first-post"),
            executed
        )
    }

    @Test
    fun `재구축 PostHook 실패 시 새로운 Registry를 유지한다`() {
        val bootstrap = RegistryBootstrap()
        val holder = TestHolder()

        var value = "Sword"

        bootstrap.submit(
            contribution(holder) {
                registry(key) {
                    add("weapon", value)
                }

                post {
                    if (value == "Diamond Sword") {
                        error("PostHook failed")
                    }
                }
            }
        )

        bootstrap.build()

        value = "Diamond Sword"

        assertFailsWith<Exception> {
            bootstrap.rebuildRegistries(holder)
        }

        assertEquals(
            "Diamond Sword",
            bootstrap.getRegistry(key)["weapon"]
        )

        assertTrue(bootstrap.isInitialized)
    }
}
