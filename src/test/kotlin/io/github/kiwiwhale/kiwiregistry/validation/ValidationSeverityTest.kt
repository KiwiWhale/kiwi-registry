package io.github.kiwiwhale.kiwiregistry.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ValidationSeverityTest {

    @Test
    fun `모든 심각도를 정의한다`() {
        assertEquals(
            setOf("ERROR", "WARNING", "INFO"),
            ValidationSeverity.entries.map { it.name }.toSet()
        )
    }

    @Test
    fun `심각도는 정의된 순서를 유지한다`() {
        assertEquals(
            listOf(
                ValidationSeverity.ERROR,
                ValidationSeverity.WARNING,
                ValidationSeverity.INFO
            ),
            ValidationSeverity.entries
        )
    }

    @Test
    fun `문자열로 Severity를 조회할 수 있다`() {
        assertEquals(
            ValidationSeverity.ERROR,
            ValidationSeverity.valueOf("ERROR")
        )

        assertEquals(
            ValidationSeverity.WARNING,
            ValidationSeverity.valueOf("WARNING")
        )

        assertEquals(
            ValidationSeverity.INFO,
            ValidationSeverity.valueOf("INFO")
        )
    }

    @Test
    fun `존재하지 않는 Severity를 조회하면 예외가 발생한다`() {
        assertFailsWith<IllegalArgumentException> {
            ValidationSeverity.valueOf("UNKNOWN")
        }
    }
}
