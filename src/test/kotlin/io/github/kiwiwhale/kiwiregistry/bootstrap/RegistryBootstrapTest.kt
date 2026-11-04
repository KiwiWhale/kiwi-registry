package io.github.kiwiwhale.kiwiregistry.bootstrap

import io.github.kiwiwhale.kiwiregistry.core.RegistryId
import io.github.kiwiwhale.kiwiregistry.core.RegistryKey
import io.github.kiwiwhale.kiwiregistry.exception.BootstrapAlreadyCompletedException
import io.github.kiwiwhale.kiwiregistry.exception.DuplicateContributionException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RegistryBootstrapTest {

    private val registryKey =
        RegistryKey.simple<String, String>(
            RegistryId("test", "items")
        )

    private class TestHolder : RegistryHolder

    private fun contribution(
        holder: RegistryHolder,
        builder: ContributionBuilder.() -> Unit
    ): Contribution {
        val contributionBuilder = ContributionBuilder()
        contributionBuilder.builder()
        return contributionBuilder.build(holder)
    }

    @Test
    fun `단일 Holder의 Registry 구축`() {
        val bootstrap = RegistryBootstrap()
        val holder = TestHolder()

        bootstrap.submit(
            contribution(holder) {
                registry(registryKey) {
                    add("sword", "Sword")
                    add("shield", "Shield")
                }
            }
        )

        bootstrap.build()

        val registry = bootstrap.getRegistry(registryKey)

        assertEquals(2, registry.size)
        assertEquals("Sword", registry["sword"])
        assertEquals("Shield", registry["shield"])
    }

    @Test
    fun `여러 Holder의 Registry 데이터를 병합한다`() {
        val bootstrap = RegistryBootstrap()

        val firstHolder = TestHolder()
        val secondHolder = TestHolder()

        bootstrap.submit(
            contribution(firstHolder) {
                registry(registryKey) {
                    add("sword", "Sword")
                }
            }
        )

        bootstrap.submit(
            contribution(secondHolder) {
                registry(registryKey) {
                    add("shield", "Shield")
                }
            }
        )

        bootstrap.build()

        val registry = bootstrap.getRegistry(registryKey)

        assertEquals(2, registry.size)
        assertEquals("Sword", registry["sword"])
        assertEquals("Shield", registry["shield"])
    }

    @Test
    fun `Holder의 Registry를 재구축한다`() {
        val bootstrap = RegistryBootstrap()
        val holder = TestHolder()

        var itemName = "Sword"

        bootstrap.submit(
            contribution(holder) {
                registry(registryKey) {
                    add("weapon", itemName)
                }
            }
        )

        bootstrap.build()

        assertEquals(
            "Sword",
            bootstrap.getRegistry(registryKey)["weapon"]
        )

        itemName = "Diamond Sword"

        bootstrap.rebuildRegistries(holder)

        assertEquals(
            "Diamond Sword",
            bootstrap.getRegistry(registryKey)["weapon"]
        )
    }

    @Test
    fun `재구축 이후 다른 Holder의 데이터가 유지된다`() {
        val bootstrap = RegistryBootstrap()

        val firstHolder = TestHolder()
        val secondHolder = TestHolder()

        var itemName = "Sword"

        bootstrap.submit(
            contribution(firstHolder) {
                registry(registryKey) {
                    add("weapon", itemName)
                }
            }
        )

        bootstrap.submit(
            contribution(secondHolder) {
                registry(registryKey) {
                    add("shield", "Shield")
                }
            }
        )

        bootstrap.build()

        itemName = "Diamond Sword"

        bootstrap.rebuildRegistries(firstHolder)

        val registry = bootstrap.getRegistry(registryKey)

        assertEquals("Diamond Sword", registry["weapon"])
        assertEquals("Shield", registry["shield"])
        assertEquals(2, registry.size)
    }

    @Test
    fun `존재하지 않는 Holder를 재구축하면 실패한다`() {
        val bootstrap = RegistryBootstrap()

        bootstrap.build()

        assertFailsWith<IllegalArgumentException> {
            bootstrap.rebuildRegistries(TestHolder())
        }
    }

    @Test
    fun `이미 구축된 Bootstrap을 다시 구축할 수 없다`() {
        val bootstrap = RegistryBootstrap()

        bootstrap.build()

        assertFailsWith<BootstrapAlreadyCompletedException> {
            bootstrap.build()
        }
    }

    @Test
    fun `동일한 Holder를 중복 등록할 수 없다`() {
        val bootstrap = RegistryBootstrap()
        val holder = TestHolder()

        bootstrap.submit(
            contribution(holder) {}
        )

        assertFailsWith<DuplicateContributionException> {
            bootstrap.submit(
                contribution(holder) {}
            )
        }
    }

    @Test
    fun `Pre Hook과 Post Hook이 실행된다`() {
        val bootstrap = RegistryBootstrap()
        val holder = TestHolder()

        val executed = ArrayList<String>()

        bootstrap.submit(
            contribution(holder) {
                pre {
                    executed += "pre"
                }

                post {
                    executed += "post"
                }

                registry(registryKey) {
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
    fun `재구축 시 대상 Holder의 Hook만 실행된다`() {
        val bootstrap = RegistryBootstrap()

        val firstHolder = TestHolder()
        val secondHolder = TestHolder()

        val executed = ArrayList<String>()

        bootstrap.submit(
            contribution(firstHolder) {
                pre { executed += "first-pre" }
                post { executed += "first-post" }

                registry(registryKey) {
                    add("sword", "Sword")
                }
            }
        )

        bootstrap.submit(
            contribution(secondHolder) {
                pre { executed += "second-pre" }
                post { executed += "second-post" }

                registry(registryKey) {
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
    fun `서로 다른 Holder가 동일한 Key를 등록하면 실패한다`() {
        val bootstrap = RegistryBootstrap()

        bootstrap.submit(
            contribution(TestHolder()) {
                registry(registryKey) {
                    add("sword", "Sword")
                }
            }
        )

        bootstrap.submit(
            contribution(TestHolder()) {
                registry(registryKey) {
                    add("sword", "Another Sword")
                }
            }
        )

        assertFailsWith<Exception> {
            bootstrap.build()
        }
    }

    @Test
    fun `재구축에 실패하면 기존 Registry를 유지한다`() {
        val bootstrap = RegistryBootstrap()
        val holder = TestHolder()

        var shouldFail = false

        bootstrap.submit(
            contribution(holder) {
                registry(registryKey) {
                    add("sword", "Sword")

                    if (shouldFail) {
                        add("sword", "Duplicate")
                    }
                }
            }
        )

        bootstrap.build()

        val original = bootstrap.getRegistry(registryKey)

        shouldFail = true

        assertFailsWith<Exception> {
            bootstrap.rebuildRegistries(holder)
        }

        val current = bootstrap.getRegistry(registryKey)

        assertEquals("Sword", current["sword"])
        assertEquals(original.data, current.data)
    }

    @Test
    fun `특정 Holder만 재구축한다`() {
        val bootstrap = RegistryBootstrap()

        val firstHolder = TestHolder()
        val secondHolder = TestHolder()

        var firstValue = "First"
        var secondBuildCount = 0

        bootstrap.submit(
            contribution(firstHolder) {
                registry(registryKey) {
                    add("first", firstValue)
                }
            }
        )

        bootstrap.submit(
            contribution(secondHolder) {
                registry(registryKey) {
                    secondBuildCount++
                    add("second", "Second")
                }
            }
        )

        bootstrap.build()

        firstValue = "Updated"

        bootstrap.rebuildRegistries(firstHolder)

        val registry = bootstrap.getRegistry(registryKey)

        assertEquals("Updated", registry["first"])
        assertEquals("Second", registry["second"])
        assertEquals(1, secondBuildCount)
    }
}
