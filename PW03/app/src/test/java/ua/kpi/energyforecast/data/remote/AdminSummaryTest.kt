package ua.kpi.energyforecast.data.remote

import org.junit.Assert.*
import org.junit.Test

class AdminSummaryTest {

    @Test
    fun acceptsValidSummary() {
        val summary = AdminSummaryDto(
            scenarioCount = 3,
            devicePositionCount = 8
        ).toSummary()

        assertEquals(3, summary.scenarioCount)
        assertEquals(8, summary.devicePositionCount)
    }

    @Test
    fun acceptsEmptyCatalog() {
        val summary = AdminSummaryDto(
            scenarioCount = 0,
            devicePositionCount = 0
        ).toSummary()

        assertEquals(AdminSummary(0, 0), summary)
    }

    @Test
    fun rejectsMissingCounters() {
        listOf(
            AdminSummaryDto(null, 8),
            AdminSummaryDto(3, null)
        ).forEach { dto ->
            assertThrows(InvalidApiDataException::class.java) {
                dto.toSummary()
            }
        }
    }

    @Test
    fun rejectsNegativeCounters() {
        listOf(
            AdminSummaryDto(-1, 8),
            AdminSummaryDto(3, -1)
        ).forEach { dto ->
            assertThrows(InvalidApiDataException::class.java) {
                dto.toSummary()
            }
        }
    }
}