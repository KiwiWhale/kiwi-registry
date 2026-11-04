package io.github.kiwiwhale.kiwiregistry.bootstrap

import io.github.kiwiwhale.kiwiregistry.RegistryKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class BootstrapTypeSafetyTest {

    @Test
    fun `일치하는 레지스트리 타입을 허용한다`() {
        val key = RegistryKey.of<String>(
            "core",
            "services"
        )

        val bootstrap = Bootstrap.create {
            registry(key) {
                add("database", "MySQL")
            }
        }

        assertEquals(
            "MySQL",
            bootstrap[key]["database"]
        )
    }

    @Test
    fun `일치하지 않는 레지스트리 타입을 거부한다`() {
        val stringKey = RegistryKey.of<String>(
            "core",
            "services"
        )

        val intKey = RegistryKey.of<Int>(
            "core",
            "services"
        )

        val bootstrap = Bootstrap.create {
            registry(stringKey) {
                add("database", "MySQL")
            }
        }

        assertFailsWith<IllegalArgumentException> {
            bootstrap[intKey]
        }
    }

    @Test
    fun `getOrNull에서 일치하지 않는 레지스트리 타입을 거부한다`() {
        val stringKey = RegistryKey.of<String>(
            "core",
            "services"
        )

        val intKey = RegistryKey.of<Int>(
            "core",
            "services"
        )

        val bootstrap = Bootstrap.create {
            registry(stringKey) {
                add("database", "MySQL")
            }
        }

        assertFailsWith<IllegalArgumentException> {
            bootstrap.getOrNull(intKey)
        }
    }

    @Test
    fun `알 수 없는 ID에 대해 null을 반환한다`() {
        val bootstrap = Bootstrap.create {}

        val key = RegistryKey.of<String>(
            "core",
            "services"
        )

        assertNull(bootstrap.getOrNull(key))
    }

    @Test
    fun `동일한 레지스트리 ID에 서로 다른 타입을 함께 등록할 수 없다`() {
        val stringKey = RegistryKey.of<String>(
            "core",
            "services"
        )

        val intKey = RegistryKey.of<Int>(
            "core",
            "services"
        )

        assertFailsWith<io.github.kiwiwhale.kiwiregistry.exception.DuplicateRegistryException> {
            Bootstrap.create {
                registry(stringKey) {
                    add("database", "MySQL")
                }

                registry(intKey) {
                    add("database", 1)
                }
            }
        }
    }

    @Test
    fun `서로 다른 레지스트리 ID에 서로 다른 타입을 지원한다`() {
        val stringKey = RegistryKey.of<String>(
            "core",
            "services"
        )

        val intKey = RegistryKey.of<Int>(
            "core",
            "commands"
        )

        val bootstrap = Bootstrap.create {
            registry(stringKey) {
                add("database", "MySQL")
            }

            registry(intKey) {
                add("reload", 1)
            }
        }

        assertEquals("MySQL", bootstrap[stringKey]["database"])
        assertEquals(1, bootstrap[intKey]["reload"])
    }
}
