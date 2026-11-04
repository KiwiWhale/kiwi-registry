package io.github.kiwiwhale.kiwiregistry.bootstrap

import io.github.kiwiwhale.kiwiregistry.RegistryKey
import io.github.kiwiwhale.kiwiregistry.exception.RegistryNotFoundException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BootstrapTest {

    private val services = RegistryKey.of<String>(
        "core",
        "services"
    )

    private val commands = RegistryKey.of<Int>(
        "core",
        "commands"
    )

    @Test
    fun `부트스트랩을 생성한다`() {
        val bootstrap = Bootstrap.create {
            registry(services) {
                add("database", "MySQL")
            }
        }

        assertEquals(1, bootstrap.size)
    }

    @Test
    fun `비어 있는 부트스트랩을 생성한다`() {
        val bootstrap = Bootstrap.create {}

        assertEquals(0, bootstrap.size)
        assertTrue(bootstrap.ids.isEmpty())
    }

    @Test
    fun `등록된 레지스트리를 반환한다`() {
        val bootstrap = Bootstrap.create {
            registry(services) {
                add("database", "MySQL")
            }
        }

        val registry = bootstrap[services]

        assertEquals("MySQL", registry["database"])
    }

    @Test
    fun `존재하지 않는 레지스트리에 대해 null을 반환한다`() {
        val bootstrap = Bootstrap.create {}

        assertNull(bootstrap.getOrNull(services))
    }

    @Test
    fun `존재하지 않는 레지스트리에 접근하면 예외를 발생시킨다`() {
        val bootstrap = Bootstrap.create {}

        assertFailsWith<RegistryNotFoundException> {
            bootstrap[services]
        }
    }

    @Test
    fun `레지스트리 ID의 등록 여부를 확인한다`() {
        val bootstrap = Bootstrap.create {
            registry(services) {
                add("database", "MySQL")
            }
        }

        assertTrue(services.id in bootstrap)
        assertFalse(commands.id in bootstrap)
    }

    @Test
    fun `모든 레지스트리 ID를 반환한다`() {
        val bootstrap = Bootstrap.create {
            registry(services) {}
            registry(commands) {}
        }

        assertEquals(
            setOf(services.id, commands.id),
            bootstrap.ids
        )
    }

    @Test
    fun `부트스트랩에 등록된 레지스트리는 동결 상태이다`() {
        val bootstrap = Bootstrap.create {
            registry(services) {
                add("database", "MySQL")
            }
        }

        val frozen = bootstrap[services]

        assertEquals("MySQL", frozen["database"])
    }

    @Test
    fun `부트스트랩에 등록된 레지스트리를 가변 상태로 변환할 수 있다`() {
        val bootstrap = Bootstrap.create {
            registry(services) {
                add("database", "MySQL")
            }
        }

        val mutable = bootstrap[services].thaw()

        mutable.register("cache", "Redis")

        assertEquals(2, mutable.size)
        assertEquals(1, bootstrap[services].size)
    }

    @Test
    fun `부트스트랩의 레지스트리 ID 집합은 수정할 수 없다`() {
        val bootstrap = Bootstrap.create {
            registry(services) {}
        }

        assertFailsWith<UnsupportedOperationException> {
            @Suppress("UNCHECKED_CAST")
            (bootstrap.ids as MutableSet<io.github.kiwiwhale.kiwiregistry.RegistryId>)
                .clear()
        }
    }
}
