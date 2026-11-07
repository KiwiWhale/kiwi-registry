package io.github.kiwiwhale.kiwiregistry.validation

import io.github.kiwiwhale.kiwiregistry.USER_REGISTRY
import io.github.kiwiwhale.kiwiregistry.User
import io.github.kiwiwhale.kiwiregistry.core.RegistryId
import io.github.kiwiwhale.kiwiregistry.core.registryKey
import io.github.kiwiwhale.kiwiregistry.dsl.registryGraph
import io.github.kiwiwhale.kiwiregistry.exception.RegistryValidationException
import kotlin.test.*

class RegistryValidatorTest {

    private val registryId = RegistryId("test", "users")

    private val userKey =
        registryKey<String, String>(registryId)

    private fun validator() =
        RegistryValidator(emptyMap())

    @Test
    fun `Error를 수집한다`() {
        val validator = validator()

        validator.error("Missing required registry.")

        val result = validator.result()

        assertTrue(result.hasErrors)
        assertFalse(result.isValid)
        assertEquals(1, result.errorCount)
        assertEquals(
            "Missing required registry.",
            result.errors.single().message
        )
    }

    @Test
    fun `Warning을 수집한다`() {
        val validator = validator()

        validator.warning("Registry is empty.")

        val result = validator.result()

        assertTrue(result.hasWarnings)
        assertTrue(result.isValid)
        assertEquals(1, result.warningCount)
    }

    @Test
    fun `Info를 수집한다`() {
        val validator = validator()

        validator.info("Validation completed.")

        val result = validator.result()

        assertTrue(result.hasInfos)
        assertTrue(result.isValid)
        assertEquals(1, result.infoCount)
    }

    @Test
    fun `여러 종류의 이슈를 함께 수집한다`() {
        val validator = validator()

        validator.error("Error")
        validator.warning("Warning")
        validator.info("Info")

        val result = validator.result()

        assertEquals(3, result.size)
        assertEquals(1, result.errorCount)
        assertEquals(1, result.warningCount)
        assertEquals(1, result.infoCount)
        assertFalse(result.isValid)
    }

    @Test
    fun `Registry ID를 이슈에 연결한다`() {
        val validator = validator()

        validator.error(
            message = "Registry is missing.",
            registryId = registryId
        )

        assertEquals(
            registryId,
            validator.result().errors.single().registryId
        )
    }

    @Test
    fun `Registry ID 없이 이슈를 수집할 수 있다`() {
        val validator = validator()

        validator.warning("General warning.")

        assertNull(
            validator.result().warnings.single().registryId
        )
    }

    @Test
    fun `Registry 존재 여부를 확인한다`() {
        val validator = validator()

        assertFalse(validator.containsRegistry(registryId))
        assertFalse(validator.containsRegistry(userKey))
    }

    @Test
    fun `존재하지 않는 Registry를 조회하면 null을 반환한다`() {
        val validator = validator()

        assertNull(validator.getOrNull(registryId))
        assertNull(validator.getOrNull(userKey))
    }

    @Test
    fun `Validation DSL에서 Error를 수집한다`() {
        val validator = validator()

        validator.validate {
            error("Invalid configuration.")
        }

        assertEquals(1, validator.result().errorCount)
    }

    @Test
    fun `Validation DSL에서 Warning을 수집한다`() {
        val validator = validator()

        validator.validate {
            warning("Potential configuration issue.")
        }

        val result = validator.result()

        assertTrue(result.isValid)
        assertEquals(1, result.warningCount)
    }

    @Test
    fun `Validation DSL에서 Info를 수집한다`() {
        val validator = validator()

        validator.validate {
            info("Validation completed.")
        }

        assertEquals(1, validator.result().infoCount)
    }

    @Test
    fun `Validation DSL에서 Registry ID를 지정할 수 있다`() {
        val validator = validator()

        validator.validate {
            error("Invalid users.", registryId)
        }

        assertEquals(
            registryId,
            validator.result().errors.single().registryId
        )
    }

    @Test
    fun `존재하지 않는 Registry를 요구하면 Error를 수집한다`() {
        val validator = validator()

        validator.validate {
            requireRegistry(userKey)
        }

        val result = validator.result()

        assertFalse(result.isValid)
        assertEquals(1, result.errorCount)
        assertEquals(registryId, result.errors.single().registryId)
    }

    @Test
    fun `존재하지 않는 Entry를 요구하면 Error를 수집한다`() {
        val validator = validator()

        validator.validate {
            requireEntry(userKey, "admin")
        }

        val result = validator.result()

        assertEquals(1, result.errorCount)
        assertEquals(registryId, result.errors.single().registryId)
    }

    @Test
    fun `존재하지 않는 Registry는 비어 있지 않음 검증에 실패한다`() {
        val validator = validator()

        validator.validate {
            assertFalse(requireNotEmpty(userKey))
        }

        assertEquals(1, validator.result().errorCount)
    }

    @Test
    fun `존재하는 Registry를 조회할 수 있다`() {
        val validator = createValidatorWithUsers(
            User("Admin", 100)
        )

        validator.validate {
            assertNotNull(getRegistry(USER_REGISTRY))
        }

        assertTrue(validator.result().isValid)
    }

    @Test
    fun `존재하는 Entry를 조회할 수 있다`() {
        val validator = createValidatorWithUsers(
            User("Admin", 100)
        )

        validator.validate {
            assertEquals(
                User("Admin", 100),
                getEntry(USER_REGISTRY, "admin")
            )
        }

        assertTrue(validator.result().isValid)
    }

    @Test
    fun `존재하는 Entry를 요구하면 값을 반환한다`() {
        val validator = createValidatorWithUsers(
            User("Admin", 100)
        )

        validator.validate {
            assertEquals(
                User("Admin", 100),
                requireEntry(USER_REGISTRY, "admin")
            )
        }

        assertTrue(validator.result().isValid)
    }

    @Test
    fun `존재하는 Registry는 비어 있지 않음 검증에 성공한다`() {
        val validator = createValidatorWithUsers(
            User("Admin", 100)
        )

        validator.validate {
            assertTrue(requireNotEmpty(USER_REGISTRY))
        }

        assertTrue(validator.result().isValid)
    }

    @Test
    fun `Registry predicate 검증에 성공한다`() {
        val validator = createValidatorWithUsers(
            User("Admin", 100)
        )

        validator.validate {
            assertTrue(
                validateRegistry(USER_REGISTRY) { users ->
                    users.isNotEmpty()
                }
            )
        }

        assertTrue(validator.result().isValid)
    }

    @Test
    fun `Registry predicate 검증에 실패하면 Error를 수집한다`() {
        val validator = createValidatorWithUsers(
            User("Admin", 100)
        )

        validator.validate {
            validateRegistry(
                key = USER_REGISTRY,
                predicate = { users -> users.isEmpty() }
            )
        }

        assertEquals(1, validator.result().errorCount)
    }

    @Test
    fun `Entry predicate 검증에 성공한다`() {
        val validator = createValidatorWithUsers(
            User("Admin", 100)
        )

        validator.validate {
            assertTrue(
                validateEntry(USER_REGISTRY, "admin") { user ->
                    user.level > 0
                }
            )
        }

        assertTrue(validator.result().isValid)
    }

    @Test
    fun `Entry predicate 검증에 실패하면 Error를 수집한다`() {
        val validator = createValidatorWithUsers(
            User("Admin", -1)
        )

        validator.validate {
            validateEntry(
                key = USER_REGISTRY,
                entryKey = "admin",
                predicate = { user -> user.level > 0 }
            )
        }

        assertEquals(1, validator.result().errorCount)
    }

    @Test
    fun `여러 Entry의 검증 실패를 모두 수집한다`() {
        val validator = createValidatorWithUsers(
            User("Admin", -1),
            User("Guest", 0),
            User("Developer", 50)
        )

        validator.validate {
            validateEntries(USER_REGISTRY) { _, user ->
                user.level > 0
            }
        }

        val result = validator.result()

        assertEquals(2, result.errorCount)

        assertEquals(
            listOf(
                "Entry 'admin' in registry 'example:users' failed validation.",
                "Entry 'guest' in registry 'example:users' failed validation."
            ),
            result.errors.map { it.message }
        )
    }

    @Test
    fun `validateEntries에서 커스텀 메시지를 사용할 수 있다`() {
        val validator = createValidatorWithUsers(
            User("Admin", -1)
        )

        validator.validate {
            validateEntries(
                key = USER_REGISTRY,
                predicate = { _, user -> user.level > 0 },
                message = { id, user ->
                    "User '$id' has invalid level: ${user.level}."
                }
            )
        }

        assertEquals(
            "User 'admin' has invalid level: -1.",
            validator.result().errors.single().message
        )
    }

    @Test
    fun `RegistryValidationException은 검증 결과를 보존한다`() {
        val validator = validator()

        validator.error("Invalid registry.")

        val result = validator.result()
        val exception = RegistryValidationException(result)

        assertSame(result, exception.result)
        assertTrue(exception.message!!.contains("1 error(s)."))
        assertTrue(exception.message!!.contains("Invalid registry."))
    }

    @Test
    fun `Error가 없는 결과로 예외를 생성할 수 없다`() {
        assertFailsWith<IllegalArgumentException> {
            RegistryValidationException(RegistryValidationResult.VALID)
        }
    }

    @Test
    fun `예외 메시지에 Warning과 Info를 포함하지 않는다`() {
        val validator = validator()

        validator.error("Error")
        validator.warning("Warning")
        validator.info("Info")

        val exception = RegistryValidationException(
            validator.result()
        )

        assertTrue(exception.message!!.contains("Error"))
        assertFalse(exception.message!!.contains("Warning"))
        assertFalse(exception.message!!.contains("Info"))
    }


    private fun createValidatorWithUsers(
        vararg users: User
    ): RegistryValidator {
        val graph = registryGraph {
            registry(USER_REGISTRY) {
                users.forEach { user ->
                    add(user.name.lowercase(), user)
                }
            }
        }

        return RegistryValidator(graph)
    }
}
