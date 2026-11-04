package io.github.kiwiwhale.kiwiregistry.registry

import io.github.kiwiwhale.kiwiregistry.bootstrap.RegistryHolder
import io.github.kiwiwhale.kiwiregistry.core.RegistryId
import io.github.kiwiwhale.kiwiregistry.core.RegistryKey
import io.github.kiwiwhale.kiwiregistry.exception.DuplicateRegistryEntryException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertNull

class RegistryTest {

    private val key = RegistryKey.simple<String, String>(
        RegistryId("test", "items")
    )

    private object TestHolder : RegistryHolder

    @Test
    fun `레지스트리에 데이터 등록`() {
        val registry = MutableRegistry(key)

        registry.register(TestHolder, "sword", "Sword")

        assertEquals("Sword", registry["sword"])
        assertEquals(1, registry.size)
    }

    @Test
    fun `등록된 데이터 제거`() {
        val registry = MutableRegistry(key)

        registry.register(TestHolder, "sword", "Sword")
        val removed = registry.unregister(TestHolder, "sword")

        assertEquals("Sword", removed)
        assertNull(registry["sword"])
        assertEquals(0, registry.size)
    }

    @Test
    fun `중복된 키 등록 금지`() {
        val registry = MutableRegistry(key)

        registry.register(TestHolder, "sword", "Sword")

        assertFailsWith<DuplicateRegistryEntryException> {
            registry.register(TestHolder, "sword", "Another Sword")
        }
    }

    @Test
    fun `레지스트리 동결`() {
        val registry = MutableRegistry(key)

        registry.register(TestHolder, "sword", "Sword")

        val frozen = registry.freeze()

        registry.register(TestHolder, "pickaxe", "Pickaxe")

        assertEquals(1, frozen.size)
        assertEquals(2, registry.size)
        assertNull(frozen["pickaxe"])
    }

    @Test
    fun `데이터 변경 시 새로운 레지스트리 생성`() {
        val registry = MutableRegistry(key)

        registry.register(TestHolder, "sword", "Sword")

        val frozen = registry.freeze()

        val rebuilt = frozen.withData(
            data = mapOf("pickaxe" to "Pickaxe"),
            holderData = emptyMap()
        )

        assertNotSame(frozen, rebuilt)
        assertEquals("Sword", frozen["sword"])
        assertEquals("Pickaxe", rebuilt["pickaxe"])
    }

    @Test
    fun `홀더별 데이터를 분리하여 보관`() {
        val registry = MutableRegistry(key)

        registry.register(TestHolder, "sword", "Sword")

        assertEquals(
            mapOf("sword" to "Sword"),
            registry.holderData[TestHolder]
        )
    }
}
