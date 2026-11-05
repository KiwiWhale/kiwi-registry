package io.github.kiwiwhale.kiwiregistry.registry

import io.github.kiwiwhale.kiwiregistry.bootstrap.RegistryHolder
import io.github.kiwiwhale.kiwiregistry.core.RegistryId
import io.github.kiwiwhale.kiwiregistry.core.RegistryKey
import io.github.kiwiwhale.kiwiregistry.exception.DuplicateRegistryEntryException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MutableRegistryTest {

    private val registryKey = RegistryKey.simple<String, String>(
        RegistryId("test", "items")
    )

    private class TestHolder : RegistryHolder

    private val holder = TestHolder()

    @Test
    fun `레지스트리에 데이터를 등록한다`() {
        val registry = MutableRegistry(registryKey)

        registry.register(holder, "sword", "Sword")

        assertEquals("Sword", registry["sword"])
        assertEquals(1, registry.size)
    }

    @Test
    fun `중복된 키 등록을 방지한다`() {
        val registry = MutableRegistry(registryKey)

        registry.register(holder, "sword", "Sword")

        assertFailsWith<DuplicateRegistryEntryException> {
            registry.register(holder, "sword", "Another Sword")
        }

        assertEquals("Sword", registry["sword"])
        assertEquals(1, registry.size)
    }

    @Test
    fun `등록된 데이터를 제거한다`() {
        val registry = MutableRegistry(registryKey)

        registry.register(holder, "sword", "Sword")

        val removed = registry.unregister(holder, "sword")

        assertEquals("Sword", removed)
        assertNull(registry["sword"])
        assertEquals(0, registry.size)
    }

    @Test
    fun `다른 Holder의 데이터는 제거하지 않는다`() {
        val registry = MutableRegistry(registryKey)

        val firstHolder = TestHolder()
        val secondHolder = TestHolder()

        registry.register(firstHolder, "sword", "Sword")
        registry.register(secondHolder, "shield", "Shield")

        registry.unregisterHolder(firstHolder)

        assertNull(registry["sword"])
        assertEquals("Shield", registry["shield"])
        assertEquals(1, registry.size)
    }

    @Test
    fun `Holder 제거 시 해당 Holder의 데이터가 모두 제거된다`() {
        val registry = MutableRegistry(registryKey)

        registry.register(holder, "sword", "Sword")
        registry.register(holder, "shield", "Shield")

        registry.unregisterHolder(holder)

        assertEquals(0, registry.size)
        assertTrue(registry.isEmpty)
        assertNull(registry.holderData[holder])
    }

    @Test
    fun `Holder별 데이터를 분리하여 조회한다`() {
        val registry = MutableRegistry(registryKey)

        val firstHolder = TestHolder()
        val secondHolder = TestHolder()

        registry.register(firstHolder, "sword", "Sword")
        registry.register(secondHolder, "shield", "Shield")

        assertEquals(
            mapOf("sword" to "Sword"),
            registry.getHolderData(firstHolder)
        )

        assertEquals(
            mapOf("shield" to "Shield"),
            registry.getHolderData(secondHolder)
        )
    }

    @Test
    fun `등록되지 않은 Holder를 제거해도 안전하다`() {
        val registry = MutableRegistry(registryKey)

        registry.unregisterHolder(holder)

        assertEquals(0, registry.size)
    }

    @Test
    fun `등록되지 않은 데이터를 제거하면 null을 반환한다`() {
        val registry = MutableRegistry(registryKey)

        assertNull(registry.unregister(holder, "sword"))
        assertEquals(0, registry.size)
    }

    @Test
    fun `Clear 이후 모든 데이터가 제거된다`() {
        val registry = MutableRegistry(registryKey)

        registry.register(holder, "sword", "Sword")
        registry.register(holder, "shield", "Shield")

        registry.clear()

        assertEquals(0, registry.size)
        assertTrue(registry.isEmpty)
        assertEquals(emptyMap(), registry.data)
        assertEquals(emptyMap(), registry.holderData)
    }

    @Test
    fun `null 값도 정상적으로 제거된다`() {
        val nullableKey = RegistryKey.simple<String, String?>(
            RegistryId("test", "nullable_items")
        )

        val registry = MutableRegistry(nullableKey)

        registry.register(holder, "empty", null)

        assertTrue("empty" in registry)

        val removed = registry.unregister(holder, "empty")

        assertNull(removed)
        assertEquals(0, registry.size)
        assertNull(registry["empty"])
        assertTrue(registry.isEmpty)
    }

    @Test
    fun `데이터 조회 결과는 외부에서 변경할 수 없다`() {
        val registry = MutableRegistry(registryKey)

        registry.register(holder, "sword", "Sword")

        @Suppress("UNCHECKED_CAST")
        val data = registry.data as MutableMap<String, String>

        assertFailsWith<UnsupportedOperationException> {
            data["shield"] = "Shield"
        }

        assertEquals(1, registry.size)
    }

    @Test
    fun `Holder 비교는 객체 동일성을 기준으로 한다`() {
        val registry = MutableRegistry(registryKey)

        val first = TestHolder()
        val second = TestHolder()

        registry.register(first, "sword", "Sword")
        registry.register(second, "shield", "Shield")

        assertEquals(1, registry.getHolderData(first).size)
        assertEquals(1, registry.getHolderData(second).size)
        assertEquals(2, registry.size)
    }

    @Test
    fun `Holder 제거 후 다른 Holder 데이터는 유지된다`() {
        val first = TestHolder()
        val second = TestHolder()

        val registry = MutableRegistry(registryKey)

        registry.register(first, "sword", "Sword")
        registry.register(second, "shield", "Shield")

        registry.unregisterHolder(first)

        assertEquals(1, registry.size)
        assertEquals("Shield", registry["shield"])
        assertNull(registry["sword"])
    }

    @Test
    fun `Freeze 이후 Holder 데이터도 독립적으로 유지된다`() {
        val registry = MutableRegistry(registryKey)

        registry.register(holder, "sword", "Sword")

        val frozen = registry.freeze()

        registry.unregisterHolder(holder)

        assertEquals(1, frozen.size)

        assertEquals(
            mapOf("sword" to "Sword"),
            frozen.getHolderData(holder)
        )
    }

    @Test
    fun `동일한 Holder 객체만 동일한 소유자로 취급한다`() {
        val first = TestHolder()
        val second = TestHolder()

        val registry = MutableRegistry(registryKey)

        registry.register(first, "sword", "Sword")
        registry.register(second, "shield", "Shield")

        assertTrue(registry.containsHolder(first))
        assertTrue(registry.containsHolder(second))

        assertEquals(2, registry.size)
    }
}
