package io.github.kiwiwhale.kiwiregistry.registry

import io.github.kiwiwhale.kiwiregistry.bootstrap.RegistryHolder
import io.github.kiwiwhale.kiwiregistry.core.RegistryId
import io.github.kiwiwhale.kiwiregistry.core.RegistryKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FrozenRegistryTest {

    private val registryKey = RegistryKey.simple<String, String>(
        RegistryId("test", "items")
    )

    private class TestHolder : RegistryHolder

    private val holder = TestHolder()

    @Test
    fun `MutableRegistry를 동결한다`() {
        val mutable = MutableRegistry(registryKey)

        mutable.register(holder, "sword", "Sword")

        val frozen = mutable.freeze()

        assertEquals(1, frozen.size)
        assertEquals("Sword", frozen["sword"])
    }

    @Test
    fun `Freeze 이후 MutableRegistry 변경이 영향을 주지 않는다`() {
        val mutable = MutableRegistry(registryKey)

        mutable.register(holder, "sword", "Sword")

        val frozen = mutable.freeze()

        mutable.register(holder, "shield", "Shield")

        assertEquals(1, frozen.size)
        assertEquals("Sword", frozen["sword"])
        assertNull(frozen["shield"])

        assertEquals(2, mutable.size)
    }

    @Test
    fun `FrozenRegistry의 데이터를 외부에서 변경할 수 없다`() {
        val mutable = MutableRegistry(registryKey)

        mutable.register(holder, "sword", "Sword")

        val frozen = mutable.freeze()

        @Suppress("UNCHECKED_CAST")
        val data = frozen.data as MutableMap<String, String>

        assertFailsWith<UnsupportedOperationException> {
            data["shield"] = "Shield"
        }

        assertEquals(1, frozen.size)
    }

    @Test
    fun `FrozenRegistry의 Holder 데이터를 외부에서 변경할 수 없다`() {
        val mutable = MutableRegistry(registryKey)

        mutable.register(holder, "sword", "Sword")

        val frozen = mutable.freeze()

        @Suppress("UNCHECKED_CAST")
        val holderData =
            frozen.holderData[holder] as MutableMap<String, String>

        assertFailsWith<UnsupportedOperationException> {
            holderData["shield"] = "Shield"
        }

        assertEquals(1, frozen.getHolderData(holder).size)
    }

    @Test
    fun `Holder별 데이터가 동결된다`() {
        val mutable = MutableRegistry(registryKey)

        mutable.register(holder, "sword", "Sword")

        val frozen = mutable.freeze()

        assertEquals(
            mapOf("sword" to "Sword"),
            frozen.getHolderData(holder)
        )
    }

    @Test
    fun `Holder가 없는 경우 빈 데이터를 반환한다`() {
        val frozen = FrozenRegistry(
            registryKey = registryKey,
            data = emptyMap(),
            holderData = emptyMap()
        )

        assertTrue(frozen.getHolderData(holder).isEmpty())
        assertEquals(false, frozen.containsHolder(holder))
    }

    @Test
    fun `빈 FrozenRegistry를 정상적으로 생성한다`() {
        val frozen = FrozenRegistry(
            registryKey = registryKey,
            data = emptyMap(),
            holderData = emptyMap()
        )

        assertEquals(0, frozen.size)
        assertTrue(frozen.isEmpty)
    }

    @Test
    fun `생성에 사용한 원본 Map 변경이 영향을 주지 않는다`() {
        val sourceData = linkedMapOf(
            "sword" to "Sword"
        )

        val sourceHolderData = linkedMapOf<RegistryHolder, Map<String, String>>(
            holder to mapOf("sword" to "Sword")
        )

        val frozen = FrozenRegistry(
            registryKey,
            sourceData,
            sourceHolderData
        )

        sourceData["shield"] = "Shield"
        sourceHolderData.clear()

        assertEquals(1, frozen.size)
        assertNull(frozen["shield"])
        assertEquals(1, frozen.holderData.size)
    }

    @Test
    fun `동일한 값을 가진 서로 다른 Holder를 구분한다`() {
        val first = TestHolder()
        val second = TestHolder()

        val frozen = FrozenRegistry(
            registryKey = registryKey,
            data = mapOf(
                "sword" to "Sword",
                "shield" to "Shield"
            ),
            holderData = linkedMapOf(
                first to mapOf("sword" to "Sword"),
                second to mapOf("shield" to "Shield")
            )
        )

        assertEquals(
            mapOf("sword" to "Sword"),
            frozen.getHolderData(first)
        )

        assertEquals(
            mapOf("shield" to "Shield"),
            frozen.getHolderData(second)
        )
    }
}
