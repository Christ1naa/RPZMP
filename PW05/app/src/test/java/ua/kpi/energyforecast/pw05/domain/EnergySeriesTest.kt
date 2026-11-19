package ua.kpi.energyforecast.pw05.domain

import org.junit.Assert.*
import org.junit.Test
import ua.kpi.energyforecast.pw05.data.DemoEnergyRepository

class EnergySeriesTest {
    @Test fun summaryAndForecastUseEnergyUnits() {
        val samples = listOf(HourlyEnergy(0, 1.0), HourlyEnergy(1, 3.0))
        val result = requireNotNull(EnergySeries.summarize(samples))
        assertEquals(4.0, result.totalKwh, 0.00001)
        assertEquals(2.0, result.meanKwh, 0.00001)
        assertEquals(3.0, result.peakKwh, 0.00001)
        assertEquals(48.0, requireNotNull(EnergySeries.forecast(samples, 24)), 0.00001)
    }
    @Test fun emptyHistoryIsMissingRatherThanZero() {
        assertNull(EnergySeries.summarize(emptyList()))
        assertNull(EnergySeries.forecast(emptyList(), 24))
    }
    @Test fun zeroConsumptionIsValid() { assertEquals(0.0, requireNotNull(EnergySeries.forecast(listOf(HourlyEnergy(0, 0.0)), 24)), 0.0) }
    @Test fun rejectsInvalidReadings() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, -1.0).forEach { value ->
            assertThrows(IllegalArgumentException::class.java) { HourlyEnergy(0, value) }
        }
        assertThrows(IllegalArgumentException::class.java) { HourlyEnergy(-1, 1.0) }
    }
    @Test fun rejectsDuplicateHoursAndOverflow() {
        assertThrows(IllegalArgumentException::class.java) { EnergySeries.summarize(listOf(HourlyEnergy(0, 1.0), HourlyEnergy(0, 2.0))) }
        assertThrows(IllegalArgumentException::class.java) { EnergySeries.summarize(listOf(HourlyEnergy(0, Double.MAX_VALUE), HourlyEnergy(1, Double.MAX_VALUE))) }
    }
    @Test fun validatesHorizon() {
        listOf(0, -1, 8761).forEach { hours -> assertThrows(IllegalArgumentException::class.java) { EnergySeries.forecast(emptyList(), hours) } }
    }
    @Test fun demoIsReproducibleAndHas24DistinctHours() {
        val repository = DemoEnergyRepository()
        val data = repository.dailyHistory()
        assertEquals(data, repository.dailyHistory())
        assertEquals((0..23).toList(), data.map { it.hour })
        assertEquals(23.6, requireNotNull(EnergySeries.summarize(data)).totalKwh, 0.00001)
    }
}
