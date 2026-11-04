package io.github.kiwiwhale.kiwiregistry.bootstrap

import io.github.kiwiwhale.kiwiregistry.RegistryKey
import io.github.kiwiwhale.kiwiregistry.exception.DuplicateEntryException
import io.github.kiwiwhale.kiwiregistry.exception.DuplicateRegistryException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BootstrapBuilderTest {

    private val services = RegistryKey.of<String>(
        "test",
        "services"
    )

    @Test
    fun `레지스트리를 선언한다`() {
        val bootstrap = Bootstrap.create {
            registry(services) {
                add("database", "MySQL")
            }
        }

        assertTrue(services.id in bootstrap)
    }

    @Test
    fun `빌더를 통해 항목을 등록한다`() {
        val bootstrap = Bootstrap.create {
            registry(services) {
                add("database", "MySQL")
                add("cache", "Redis")
            }
        }

        val registry = bootstrap[services]

        assertEquals(2, registry.size)
        assertEquals("MySQL", registry["database"])
        assertEquals("Redis", registry["cache"])
    }

    @Test
    fun `여러 항목을 등록한다`() {
        val bootstrap = Bootstrap.create {
            registry(services) {
                addAll(
                    mapOf(
                        "database" to "MySQL",
                        "cache" to "Redis"
                    )
                )
            }
        }

        assertEquals(2, bootstrap[services].size)
    }

    @Test
    fun `중복된 레지스트리 선언을 거부한다`() {
        assertFailsWith<DuplicateRegistryException> {
            Bootstrap.create {
                registry(services) {
                    add("database", "MySQL")
                }

                registry(services) {
                    add("cache", "Redis")
                }
            }
        }
    }

    @Test
    fun `중복된 항목 등록을 거부한다`() {
        assertFailsWith<IllegalStateException> {
            Bootstrap.create {
                registry(services) {
                    add("database", "MySQL")
                    add("database", "PostgreSQL")
                }
            }
        }
    }

    @Test
    fun `중복 항목이 존재하면 일괄 등록을 원자적으로 처리한다`() {
        val bootstrap = Bootstrap.create {
            registry(services) {
                add("database", "MySQL")

                assertFailsWith<IllegalStateException> {
                    addAll(
                        linkedMapOf(
                            "cache" to "Redis",
                            "database" to "PostgreSQL"
                        )
                    )
                }

                assertFalse("cache" in keys())
                assertEquals(1, size)
            }
        }

        assertEquals(1, bootstrap[services].size)
    }

    @Test
    fun `잘못된 항목 키를 거부한다`() {
        val invalidKeys = listOf(
            "",
            " ",
            "database:primary"
        )

        invalidKeys.forEach { invalidKey ->
            assertFailsWith<IllegalArgumentException> {
                Bootstrap.create {
                    registry(services) {
                        add(invalidKey, "MySQL")
                    }
                }
            }
        }
    }

    @Test
    fun `비어 있는 레지스트리를 지원한다`() {
        val bootstrap = Bootstrap.create {
            registry(services) {}
        }

        assertTrue(bootstrap[services].isEmpty)
    }

    @Test
    fun `서로 다른 여러 레지스트리를 지원한다`() {
        val commands = RegistryKey.of<Int>(
            "test",
            "commands"
        )

        val bootstrap = Bootstrap.create {
            registry(services) {
                add("database", "MySQL")
            }

            registry(commands) {
                add("reload", 1)
            }
        }

        assertEquals(2, bootstrap.size)
        assertEquals("MySQL", bootstrap[services]["database"])
        assertEquals(1, bootstrap[commands]["reload"])
    }

    @Test
    fun `부트스트랩 생성 이후 빌더 재사용을 거부한다`() {
        lateinit var captured: BootstrapBuilder

        Bootstrap.create {
            captured = this

            registry(services) {
                add("database", "MySQL")
            }
        }

        assertFailsWith<IllegalStateException> {
            captured.registry(
                RegistryKey.of<Int>("test", "commands")
            ) {
                add("reload", 1)
            }
        }
    }
}
