package io.github.kiwiwhale.kiwiregistry.validation

import kotlin.test.*

class RegistryValidationResultTest {

    @Test
    fun `검증 이슈가 없으면 VALID를 반환한다`() {
        val result = RegistryValidationResult.VALID

        assertTrue(result.isValid)
        assertTrue(result.isEmpty)
        assertEquals(0, result.size)
        assertEquals(0, result.errorCount)
        assertEquals(0, result.warningCount)
        assertEquals(0, result.infoCount)
    }

    @Test
    fun `Error가 존재하면 검증에 실패한다`() {
        val result = RegistryValidationResult(
            listOf(
                ValidationIssue(ValidationSeverity.ERROR, "Error")
            )
        )

        assertFalse(result.isValid)
        assertTrue(result.hasErrors)
        assertEquals(1, result.errorCount)
    }

    @Test
    fun `Warning과 Info만 있으면 검증에 성공한다`() {
        val result = RegistryValidationResult(
            listOf(
                ValidationIssue(ValidationSeverity.WARNING, "Warning"),
                ValidationIssue(ValidationSeverity.INFO, "Info")
            )
        )

        assertTrue(result.isValid)
        assertFalse(result.hasErrors)
        assertTrue(result.hasWarnings)
        assertTrue(result.hasInfos)
    }

    @Test
    fun `여러 심각도의 이슈를 정확하게 집계한다`() {
        val result = RegistryValidationResult(
            listOf(
                ValidationIssue(ValidationSeverity.ERROR, "Error 1"),
                ValidationIssue(ValidationSeverity.WARNING, "Warning"),
                ValidationIssue(ValidationSeverity.INFO, "Info"),
                ValidationIssue(ValidationSeverity.ERROR, "Error 2")
            )
        )

        assertEquals(4, result.size)
        assertEquals(2, result.errorCount)
        assertEquals(1, result.warningCount)
        assertEquals(1, result.infoCount)
    }

    @Test
    fun `전체 이슈의 발견 순서를 유지한다`() {
        val result = RegistryValidationResult(
            listOf(
                ValidationIssue(ValidationSeverity.INFO, "First"),
                ValidationIssue(ValidationSeverity.ERROR, "Second"),
                ValidationIssue(ValidationSeverity.WARNING, "Third")
            )
        )

        assertEquals(
            listOf("First", "Second", "Third"),
            result.issues.map { it.message }
        )
    }

    @Test
    fun `심각도별 이슈 순서를 유지한다`() {
        val result = RegistryValidationResult(
            listOf(
                ValidationIssue(ValidationSeverity.WARNING, "Warning 1"),
                ValidationIssue(ValidationSeverity.ERROR, "Error 1"),
                ValidationIssue(ValidationSeverity.WARNING, "Warning 2"),
                ValidationIssue(ValidationSeverity.ERROR, "Error 2")
            )
        )

        assertEquals(
            listOf("Error 1", "Error 2"),
            result.errors.map { it.message }
        )

        assertEquals(
            listOf("Warning 1", "Warning 2"),
            result.warnings.map { it.message }
        )
    }

    @Test
    fun `전체 이슈 목록은 수정할 수 없다`() {
        val result = RegistryValidationResult(
            listOf(
                ValidationIssue(ValidationSeverity.ERROR, "Error")
            )
        )

        @Suppress("UNCHECKED_CAST")
        val mutable = result.issues as MutableList<ValidationIssue>

        assertFailsWith<UnsupportedOperationException> {
            mutable.add(
                ValidationIssue(ValidationSeverity.INFO, "Another")
            )
        }
    }

    @Test
    fun `심각도별 이슈 목록은 수정할 수 없다`() {
        val result = RegistryValidationResult(
            listOf(
                ValidationIssue(ValidationSeverity.ERROR, "Error")
            )
        )

        @Suppress("UNCHECKED_CAST")
        val mutable = result.errors as MutableList<ValidationIssue>

        assertFailsWith<UnsupportedOperationException> {
            mutable.clear()
        }
    }

    @Test
    fun `이전 결과는 이후 변경에 영향을 받지 않는다`() {
        val issues = mutableListOf(
            ValidationIssue(ValidationSeverity.ERROR, "First")
        )

        val previous = RegistryValidationResult(issues)

        issues.add(
            ValidationIssue(ValidationSeverity.WARNING, "Second")
        )

        assertEquals(1, previous.size)
        assertEquals(0, previous.warningCount)
    }

    @Test
    fun `동일한 이슈를 가진 결과는 동등하다`() {
        val first = RegistryValidationResult(
            listOf(
                ValidationIssue(ValidationSeverity.ERROR, "Error")
            )
        )

        val second = RegistryValidationResult(
            listOf(
                ValidationIssue(ValidationSeverity.ERROR, "Error")
            )
        )

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

    @Test
    fun `이슈의 순서가 다르면 결과도 다르다`() {
        val first = RegistryValidationResult(
            listOf(
                ValidationIssue(ValidationSeverity.ERROR, "First"),
                ValidationIssue(ValidationSeverity.WARNING, "Second")
            )
        )

        val second = RegistryValidationResult(
            listOf(
                ValidationIssue(ValidationSeverity.WARNING, "Second"),
                ValidationIssue(ValidationSeverity.ERROR, "First")
            )
        )

        assertNotEquals(first, second)
    }

    @Test
    fun `이슈가 없는 결과는 isEmpty가 true다`() {
        assertTrue(RegistryValidationResult.VALID.isEmpty)
    }
}
