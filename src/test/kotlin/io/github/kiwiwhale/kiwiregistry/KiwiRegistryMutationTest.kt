package io.github.kiwiwhale.kiwiregistry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class KiwiRegistryMutationTest {

    private data class Value(
        val value: String
    )

    private class Key(
        override val id: RegistryId
    ) : RegistryKey<Value>

    @Test
    fun `set은 값을 덮어쓴다`() {
        val registry = KiwiRegistry<Value>("test")

        registry["user"] = Value("first")
        registry["user"] = Value("second")

        assertEquals(Value("second"), registry["user"])
        assertEquals(1, registry.size)
    }

    @Test
    fun `set ID는 기존 Key를 유지한다`() {
        val registry = KiwiRegistry<Value>("test")
        val original = Key(RegistryId("test", "user"))

        registry.register(original, Value("first"))
        registry[RegistryId("test", "user")] = Value("second")

        assertEquals(setOf(original), registry.keys())
        assertEquals(Value("second"), registry["user"])
    }

    @Test
    fun `set Key는 Key 정보를 교체한다`() {
        val registry = KiwiRegistry<Value>("test")

        val original = Key(RegistryId("test", "user"))
        val replacement = KiwiRegistryKey<Value>(
            RegistryId("test", "user")
        )

        registry.register(original, Value("first"))
        registry[replacement] = Value("second")

        assertEquals(setOf(replacement), registry.keys())
        assertNotEquals(original, registry.keys().first())
        assertEquals(Value("second"), registry["user"])
    }

    @Test
    fun `replace는 기존 값만 변경한다`() {
        val registry = KiwiRegistry<Value>("test")
        val key = Key(RegistryId("test", "user"))

        registry.register(key, Value("first"))

        assertTrue(
            registry.replace("user", Value("second"))
        )

        assertEquals(Value("second"), registry["user"])
        assertEquals(setOf(key), registry.keys())
    }

    @Test
    fun `replace는 존재하지 않는 ID에 대해 false를 반환한다`() {
        val registry = KiwiRegistry<Value>("test")

        assertFalse(
            registry.replace("user", Value("value"))
        )

        assertTrue(registry.isEmpty())
    }

    @Test
    fun `unregister는 항목을 제거한다`() {
        val registry = KiwiRegistry<Value>("test")

        registry.register("user", Value("first"))

        assertTrue(registry.unregister("user"))

        assertFalse("user" in registry)
        assertEquals(0, registry.size)
    }

    @Test
    fun `unregister는 존재하지 않는 항목에 대해 false를 반환한다`() {
        val registry = KiwiRegistry<Value>("test")

        assertFalse(registry.unregister("unknown"))
    }

    @Test
    fun `clear는 모든 항목을 제거한다`() {
        val registry = KiwiRegistry<Value>("test")

        registry.register("user", Value("first"))
        registry.register("admin", Value("second"))

        registry.clear()

        assertTrue(registry.isEmpty())
        assertEquals(0, registry.size)
        assertTrue(registry.keys().isEmpty())
        assertTrue(registry.ids().isEmpty())
        assertTrue(registry.names().isEmpty())
    }

    @Test
    fun `clear는 빈 Registry에서도 안전하다`() {
        val registry = KiwiRegistry<Value>("test")

        registry.clear()

        assertTrue(registry.isEmpty())
    }

    @Test
    fun `set은 새로운 항목도 등록한다`() {
        val registry = KiwiRegistry<Value>("test")

        registry["user"] = Value("first")

        assertEquals(1, registry.size)
        assertEquals(Value("first"), registry["user"])
    }

    @Test
    fun `set Key는 기존 항목의 Key와 Value를 교체한다`() {
        val registry = KiwiRegistry<Value>("test")

        val original = Key(RegistryId("test", "user"))
        val replacement = KiwiRegistryKey<Value>(
            RegistryId("test", "user")
        )

        registry.register(original, Value("first"))
        registry[replacement] = Value("second")

        assertEquals(1, registry.size)
        assertEquals(replacement, registry.keys().single())
        assertEquals(Value("second"), registry["user"])
    }
}
