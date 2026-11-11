package ua.kpi.energyforecast.pw04

import org.junit.Assert.*
import org.junit.Test
import ua.kpi.energyforecast.pw04.data.DemoSites
import ua.kpi.energyforecast.pw04.logic.*
import ua.kpi.energyforecast.pw04.model.*
import java.time.Instant

class SiteQueryTest {
    private val readings = DemoSites.sites.map { SiteReading(it, 10.0, 100.0, Instant.EPOCH) }

    @Test fun combinedFiltersUseAndAndSearchIgnoresCase() {
        val result = querySites(readings, SiteQuery(search = " ОФІС ", type = SiteType.OFFICE,
            status = SiteStatus.ACTIVE, minPowerKw = "30,0", maxPowerKw = "50"))
        assertEquals(listOf("office-1"), result.map { it.site.id })
    }

    @Test fun contradictoryFiltersAndEmptyInputProduceEmptyResults() {
        assertTrue(querySites(readings, SiteQuery(search = "Цех", type = SiteType.HOME)).isEmpty())
        assertTrue(querySites(emptyList(), SiteQuery()).isEmpty())
    }

    @Test fun powerBoundsAreInclusive() {
        assertEquals(listOf("home-1"), querySites(readings, SiteQuery(minPowerKw = "12", maxPowerKw = "12")).map { it.site.id })
    }

    @Test fun invalidRangesNeverSilentlyDisableFiltering() {
        listOf("NaN", "Infinity", "-1", "abc").forEach { bad ->
            val query = SiteQuery(minPowerKw = bad)
            assertNotNull(query.error)
            assertTrue(querySites(readings, query).isEmpty())
            assertNotNull(SiteQuery(maxPowerKw = bad).error)
        }
        assertNotNull(SiteQuery(minPowerKw = "10", maxPowerKw = "2").error)
    }

    @Test fun secondarySortResolvesPrimaryTiesAndDirectionAppliesToBoth() {
        val query = SiteQuery(primary = SiteSort.TYPE, secondary = SiteSort.POWER)
        val ascending = querySites(readings, query)
        val offices = ascending.filter { it.site.type == SiteType.OFFICE }
        assertEquals(listOf(28.0, 45.0), offices.map { it.site.ratedPowerKw })
        assertEquals(ascending.reversed().map { it.site.id }, querySites(readings, query.copy(descending = true)).map { it.site.id })
    }

    @Test fun numericAndDateSortingAreNotLexical() {
        assertEquals(200.0, querySites(readings, SiteQuery(primary = SiteSort.POWER, descending = true)).first().site.ratedPowerKw, 0.0)
        assertEquals("factory-2", querySites(readings, SiteQuery(primary = SiteSort.DATE, descending = true)).first().site.id)
    }

    @Test fun queryDoesNotMutateTheSource() {
        val original = readings.toList()
        querySites(readings, SiteQuery(type = SiteType.HOME, descending = true))
        assertEquals(original, readings)
    }

    @Test fun forecastUsesEnergyUnitsAndZeroIsValid() {
        assertEquals(88.0, forecastDaily(10.0, 8.0, 1.1), 1e-9)
        assertEquals(0.0, forecastDaily(0.0, 24.0, 1.1), 0.0)
    }

    @Test fun invalidForecastAndCoordinatesAreRejected() {
        listOf<() -> Any>(
            { forecastDaily(Double.NaN, 8.0, 1.0) },
            { forecastDaily(1.0, 25.0, 1.0) },
            { forecastDaily(1.0, 8.0, -1.0) },
            { forecastDaily(Double.MAX_VALUE, 24.0, 2.0) },
            { Coordinates(91.0, 0.0) }, { Coordinates(0.0, Double.NaN) }
        ).forEach { action -> assertThrows(IllegalArgumentException::class.java) { action() } }
    }
}
