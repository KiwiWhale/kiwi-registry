package io.github.kiwiwhale.kiwiregistry.validation

import io.github.kiwiwhale.kiwiregistry.core.RegistryId
import kotlin.test.*

class ValidationIssueTest {

    private val registryId = RegistryId("test", "users")

    @Test
    fun `정상적인 Issue를 생성한다`() {
        val issue = ValidationIssue(
            ValidationSeverity.ERROR,
            "Invalid registry."
        )

        assertEquals(ValidationSeverity.ERROR, issue.severity)
        assertEquals("Invalid registry.", issue.message)
        assertNull(issue.registryId)
    }

    @Test
    fun `Registry ID를 연결한 Issue를 생성한다`() {
        val issue = ValidationIssue(
            ValidationSeverity.ERROR,
            "Invalid registry.",
            registryId
        )

        assertEquals(registryId, issue.registryId)
    }

    @Test
    fun `빈 메시지로 생성하면 예외를 발생시킨다`() {
        assertFailsWith<IllegalArgumentException> {
            ValidationIssue(
                ValidationSeverity.ERROR,
                ""
            )
        }
    }

    @Test
    fun `공백 메시지로 생성하면 예외를 발생시킨다`() {
        assertFailsWith<IllegalArgumentException> {
            ValidationIssue(
                ValidationSeverity.WARNING,
                "   "
            )
        }
    }

    @Test
    fun `개행 문자만 있는 메시지로 생성하면 예외를 발생시킨다`() {
        assertFailsWith<IllegalArgumentException> {
            ValidationIssue(
                ValidationSeverity.INFO,
                "\n\t"
            )
        }
    }

    @Test
    fun `메시지 앞뒤 공백이 있는 경우에도 정상적으로 생성한다`() {
        val issue = ValidationIssue(
            ValidationSeverity.INFO,
            "  Information  "
        )

        assertEquals("  Information  ", issue.message)
    }

    @Test
    fun `동일한 속성을 가진 Issue는 동등하다`() {
        val first = ValidationIssue(
            ValidationSeverity.ERROR,
            "Error",
            registryId
        )

        val second = ValidationIssue(
            ValidationSeverity.ERROR,
            "Error",
            registryId
        )

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `Severity가 다르면 Issue는 동등하지 않다`() {
        val first = ValidationIssue(
            ValidationSeverity.ERROR,
            "Issue"
        )

        val second = ValidationIssue(
            ValidationSeverity.WARNING,
            "Issue"
        )

        assertNotEquals(first, second)
    }

    @Test
    fun `Registry ID가 다르면 Issue는 동등하지 않다`() {
        val first = ValidationIssue(
            ValidationSeverity.ERROR,
            "Error",
            registryId
        )

        val second = ValidationIssue(
            ValidationSeverity.ERROR,
            "Error",
            RegistryId("test", "products")
        )

        assertNotEquals(first, second)
    }
}
