package ua.kpi.doctrack.model

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class RecordDatesTest {
    @Test fun rejectsYearZeroAndUnsupportedYears() {
        assertNull(RecordDates.parse("01.01.0000"))
        listOf(-1, 0, 10000).forEach {
            assertFalse(RecordDates.isSupported(LocalDate.of(it, 1, 1)))
        }
    }

    @Test fun supportedYearBoundariesRoundTripWithoutEraConversion() {
        listOf("01.01.0001", "31.12.9999", "29.02.2024").forEach { text ->
            val date = RecordDates.parse(text)
            assertNotNull(text, date)
            assertEquals(text, date!!.format(RecordDates.formatter))
        }
    }

    @Test fun rejectsImpossibleDatesAndWrongFormat() {
        listOf("31.02.2026", "29.02.2025", "1.01.2026", "2026-01-01", "01.01.10000").forEach {
            assertNull(it, RecordDates.parse(it))
        }
    }

    @Test fun strictLeapYearRulesArePreserved() {
        assertNull(RecordDates.parse("29.02.1900"))
        assertEquals(LocalDate.of(2000, 2, 29), RecordDates.parse("29.02.2000"))
    }
}
