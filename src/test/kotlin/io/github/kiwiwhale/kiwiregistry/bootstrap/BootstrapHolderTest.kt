package io.github.kiwiwhale.kiwiregistry.bootstrap

import io.github.kiwiwhale.kiwiregistry.core.RegistryId
import io.github.kiwiwhale.kiwiregistry.core.RegistryKey
import io.github.kiwiwhale.kiwiregistry.exception.BootstrapAlreadyInjectedException
import io.github.kiwiwhale.kiwiregistry.exception.BootstrapNotCompletedException
import io.github.kiwiwhale.kiwiregistry.exception.BootstrapNotInjectedException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class BootstrapHolderTest {

    private val registryKey = RegistryKey.simple<String, String>(
        RegistryId("test", "items")
    )

    private class TestBootstrapHolder : BootstrapHolder

    private object TestRegistryHolder : RegistryHolder

    @Test
    fun `BootstrapHolder를 정상적으로 주입한다`() {
        val holder = TestBootstrapHolder()

        val build = holder.inject()

        assertNotNull(build)
    }

    @Test
    fun `동일한 BootstrapHolder의 중복 주입을 방지한다`() {
        val holder = TestBootstrapHolder()

        holder.inject()

        assertFailsWith<BootstrapAlreadyInjectedException> {
            holder.inject()
        }
    }

    @Test
    fun `주입하지 않은 BootstrapHolder의 Registry 조회를 방지한다`() {
        val holder = TestBootstrapHolder()

        assertFailsWith<BootstrapNotInjectedException> {
            holder.registry(registryKey)
        }
    }

    @Test
    fun `Bootstrap 빌드 후 Registry를 조회한다`() {
        val holder = TestBootstrapHolder()

        val build = holder.inject()

        TestRegistryHolder.registryBootstrap(holder) {
            registry(registryKey) {
                add("sword", "Sword")
            }
        }

        build()

        val registry = holder.registry(registryKey)

        assertEquals("Sword", registry["sword"])
        assertEquals(1, registry.size)
    }

    @Test
    fun `연산자로 Registry를 조회한다`() {
        val holder = TestBootstrapHolder()

        val build = holder.inject()

        TestRegistryHolder.registryBootstrap(holder) {
            registry(registryKey) {
                add("sword", "Sword")
            }
        }

        build()

        val registry = holder[registryKey]

        assertEquals("Sword", registry["sword"])
    }

    @Test
    fun `빌드 전 Registry 조회를 방지한다`() {
        val holder = TestBootstrapHolder()

        holder.inject()

        assertFailsWith<BootstrapNotCompletedException> {
            holder.registry(registryKey)
        }
    }
}
