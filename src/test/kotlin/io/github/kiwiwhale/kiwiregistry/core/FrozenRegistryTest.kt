package io.github.kiwiwhale.kiwiregistry.core

import io.github.kiwiwhale.kiwiregistry.RegistryId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FrozenRegistryTest {

    private fun createMutable() =
        MutableRegistry.create<String>(
            RegistryId("test", "services")
        )

    @Test
    fun `가변 레지스트리를 동결한다`() {
        val mutable = createMutable()

        mutable.register("database", "MySQL")

        val frozen = mutable.freeze()

        assertEquals(1, frozen.size)
        assertEquals("MySQL", frozen["database"])
    }

    @Test
    fun `동결된 레지스트리는 원래 상태를 유지한다`() {
        val mutable = createMutable()

        mutable.register("database", "MySQL")

        val frozen = mutable.freeze()

        mutable.register("cache", "Redis")

        assertEquals(1, frozen.size)
        assertEquals(2, mutable.size)

        assertFalse("cache" in frozen)
        assertTrue("cache" in mutable)
    }

    @Test
    fun `동결된 레지스트리를 가변 레지스트리로 변환한다`() {
        val mutable = createMutable()

        mutable.register("database", "MySQL")

        val frozen = mutable.freeze()
        val restored = frozen.thaw()

        assertEquals(frozen.id, restored.id)
        assertEquals(frozen.snapshot(), restored.snapshot())
    }

    @Test
    fun `변환된 레지스트리는 수정할 수 있다`() {
        val mutable = createMutable()

        mutable.register("database", "MySQL")

        val restored = mutable.freeze().thaw()

        restored.register("cache", "Redis")

        assertEquals("Redis", restored["cache"])
    }

    @Test
    fun `가변 레지스트리로 변환해도 동결된 레지스트리는 변경되지 않는다`() {
        val mutable = createMutable()

        mutable.register("database", "MySQL")

        val frozen = mutable.freeze()
        val restored = frozen.thaw()

        restored.register("cache", "Redis")
        restored.unregister("database")

        assertTrue("database" in frozen)
        assertFalse("cache" in frozen)
        assertFalse("database" in restored)
    }

    @Test
    fun `가변 레지스트리와 동결된 레지스트리 간 반복 변환을 지원한다`() {
        val mutable = createMutable()

        mutable.register("first", "1")

        val frozen1 = mutable.freeze()
        val mutable2 = frozen1.thaw()

        mutable2.register("second", "2")

        val frozen2 = mutable2.freeze()
        val mutable3 = frozen2.thaw()

        mutable3.register("third", "3")

        assertEquals(1, frozen1.size)
        assertEquals(2, frozen2.size)
        assertEquals(3, mutable3.size)
    }

    @Test
    fun `동결된 레지스트리를 직접 생성한다`() {
        val frozen = FrozenRegistry.of(
            id = RegistryId("test", "services"),
            entries = mapOf(
                "database" to "MySQL",
                "cache" to "Redis"
            )
        )

        assertEquals(2, frozen.size)
        assertEquals("MySQL", frozen["database"])
        assertEquals("Redis", frozen["cache"])
    }

    @Test
    fun `동결된 레지스트리에서 포함 여부와 항목 조회를 지원한다`() {
        val frozen = FrozenRegistry.of(
            RegistryId("test", "services"),
            mapOf("database" to "MySQL")
        )

        assertTrue("database" in frozen)
        assertFalse("cache" in frozen)
        assertEquals("MySQL", frozen.getOrNull("database"))
        assertNull(frozen.getOrNull("cache"))
    }

    @Test
    fun `비어 있는 동결된 레지스트리를 가변 레지스트리로 변환할 수 있다`() {
        val frozen = FrozenRegistry.of<String>(
            RegistryId("test", "empty"),
            emptyMap()
        )

        val mutable = frozen.thaw()

        assertTrue(mutable.isEmpty)

        mutable.register("entry", "value")

        assertEquals("value", mutable["entry"])
        assertTrue(frozen.isEmpty)
    }

    @Test
    fun `동결된 레지스트리 생성 시 잘못된 키를 거부한다`() {
        assertFailsWith<IllegalArgumentException> {
            FrozenRegistry.of(
                RegistryId("test", "services"),
                mapOf("" to "invalid")
            )
        }

        assertFailsWith<IllegalArgumentException> {
            FrozenRegistry.of(
                RegistryId("test", "services"),
                mapOf("invalid:key" to "invalid")
            )
        }
    }
}
