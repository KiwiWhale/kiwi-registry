package io.github.kiwiwhale.kiwiregistry.bootstrap

import io.github.kiwiwhale.kiwiregistry.core.RegistryId
import io.github.kiwiwhale.kiwiregistry.core.RegistryKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RegistryBootstrapTest {

    private val key = RegistryBootstrapTestFixture.registryKey

    private val contribution = RegistryBootstrapTestFixture::contribution

    private class TestHolder : RegistryHolder

    @Test
    fun `단일 Holder의 Registry를 구축한다`() {
        val bootstrap = RegistryBootstrap()

        bootstrap.submit(
            contribution(TestHolder()) {
                registry(key) {
                    add("sword", "Sword")
                    add("shield", "Shield")
                }
            }
        )

        bootstrap.build()

        val registry = bootstrap.getRegistry(key)

        assertEquals(2, registry.size)
        assertEquals("Sword", registry["sword"])
        assertEquals("Shield", registry["shield"])
        assertTrue(bootstrap.isInitialized)
    }

    @Test
    fun `여러 Holder의 Registry 데이터를 병합한다`() {
        val bootstrap = RegistryBootstrap()

        bootstrap.submit(
            contribution(TestHolder()) {
                registry(key) {
                    add("sword", "Sword")
                }
            }
        )

        bootstrap.submit(
            contribution(TestHolder()) {
                registry(key) {
                    add("shield", "Shield")
                }
            }
        )

        bootstrap.build()

        val registry = bootstrap.getRegistry(key)

        assertEquals(2, registry.size)
        assertEquals("Sword", registry["sword"])
        assertEquals("Shield", registry["shield"])
    }

    @Test
    fun `Registry가 없는 Contribution도 구축할 수 있다`() {
        val bootstrap = RegistryBootstrap()

        bootstrap.submit(
            contribution(TestHolder()) {}
        )

        bootstrap.build()

        assertTrue(bootstrap.isInitialized)
    }

    @Test
    fun `Contribution이 없는 Bootstrap도 구축할 수 있다`() {
        val bootstrap = RegistryBootstrap()

        bootstrap.build()

        assertTrue(bootstrap.isInitialized)
    }

    @Test
    fun `서로 다른 RegistryKey를 독립적으로 구축한다`() {
        val bootstrap = RegistryBootstrap()

        val weapons = RegistryKey.simple<String, String>(
            RegistryId("test", "weapons")
        )

        val armors = RegistryKey.simple<String, String>(
            RegistryId("test", "armors")
        )

        bootstrap.submit(
            contribution(TestHolder()) {
                registry(weapons) {
                    add("sword", "Sword")
                }

                registry(armors) {
                    add("shield", "Shield")
                }
            }
        )

        bootstrap.build()

        assertEquals(
            "Sword",
            bootstrap.getRegistry(weapons)["sword"]
        )

        assertEquals(
            "Shield",
            bootstrap.getRegistry(armors)["shield"]
        )

        assertEquals(2, bootstrap.graph().size)
    }
}
