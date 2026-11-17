package ua.kpi.energyforecast.pw04

import org.junit.Assert.*
import org.junit.Test
import ua.kpi.energyforecast.pw04.data.DemoSites
import ua.kpi.energyforecast.pw04.logic.*
import ua.kpi.energyforecast.pw04.model.*
import java.time.Instant

class SiteQueryAdvancedTest {
    private fun reading(id: String, name: String, type: SiteType = SiteType.HOME,
                        power: Double = 20.0, status: SiteStatus = SiteStatus.ACTIVE,
                        day: Long = 1, forecast: Double = 30.0) = SiteReading(
        DemoSites.sites.first().copy(id = id, name = name, type = type, ratedPowerKw = power,
            status = status, updatedAt = Instant.EPOCH.plusSeconds(day * 86400)),
        1.0, forecast, Instant.EPOCH)

    private val fixture = listOf(
        reading("a", "Гамма", power = 100.0, day = 1, forecast = 30.0),
        reading("b", "Ґанок", SiteType.OFFICE, 20.0, SiteStatus.MAINTENANCE, 3, 2.0),
        reading("c", "Дім", SiteType.INDUSTRY, 3.0, day = 2, forecast = 100.0),
        reading("d", "Іскра", power = 20.0, day = 4, forecast = 40.0),
        reading("e", "Їжак", SiteType.OFFICE, 5.0, day = 5, forecast = 10.0)
    )

    @Test fun everyPrimaryCriterionHasKnownAscendingAndDescendingOrder() {
        val expected = mapOf(
            SiteSort.NAME to listOf("a", "b", "c", "d", "e"),
            SiteSort.TYPE to listOf("c", "a", "d", "b", "e"),
            SiteSort.POWER to listOf("c", "e", "b", "d", "a"),
            SiteSort.DATE to listOf("a", "c", "b", "d", "e"),
            SiteSort.STATUS to listOf("a", "c", "d", "e", "b"),
            SiteSort.FORECAST to listOf("b", "e", "a", "d", "c")
        )
        expected.forEach { (sort, ids) ->
            val query = SiteQuery(primary = sort, secondary = SiteSort.NAME)
            assertEquals(sort.name, ids, querySites(fixture.reversed(), query).map { it.site.id })
            assertEquals(sort.name, ids.reversed(), querySites(fixture, query.copy(descending = true)).map { it.site.id })
        }
    }

    @Test fun everySecondaryCriterionResolvesATieBeforeId() {
        SiteSort.entries.forEach { secondary ->
            val primary = if (secondary == SiteSort.POWER) SiteSort.NAME else SiteSort.POWER
            val left = reading("z", if (secondary == SiteSort.POWER) "Станція" else "Альфа",
                SiteType.INDUSTRY, 10.0, SiteStatus.ACTIVE, 1, 1.0)
            val right = reading("a", if (secondary == SiteSort.POWER) "Станція" else "Явір",
                SiteType.OFFICE, if (secondary == SiteSort.POWER) 20.0 else 10.0, SiteStatus.MAINTENANCE, 2, 2.0)
            val query = SiteQuery(primary = primary, secondary = secondary)
            assertEquals(secondary.name, listOf("z", "a"), querySites(listOf(right, left), query).map { it.site.id })
            assertEquals(secondary.name, listOf("a", "z"), querySites(listOf(left, right), query.copy(descending = true)).map { it.site.id })
        }
    }

    @Test fun equalValuesAndCaseVariantsUseStableIdInBothDirections() {
        val a = reading("a", "Енергія")
        val z = reading("z", "енергія")
        listOf(false, true).forEach { descending ->
            listOf(SiteSort.NAME, SiteSort.POWER).forEach { secondary ->
                val query = SiteQuery(primary = SiteSort.NAME, secondary = secondary, descending = descending)
                listOf(listOf(a, z), listOf(z, a)).forEach { input ->
                    assertEquals(listOf("a", "z"), querySites(input, query).map { it.site.id })
                }
            }
        }
    }

    @Test fun ukrainianAlphabetIncludesGheAndYiInTheirCorrectPositions() {
        val names = listOf("Явір", "Ґанок", "Ганок", "Дім", "Єдність", "Енергія", "Житло", "Їжак", "Іскра", "Ирій", "Йод")
        val input = names.mapIndexed { index, name -> reading(index.toString(), name) }
        assertEquals(listOf("Ганок", "Ґанок", "Дім", "Енергія", "Єдність", "Житло", "Ирій", "Іскра", "Їжак", "Йод", "Явір"),
            querySites(input, SiteQuery()).map { it.site.name })
    }

    @Test fun searchNormalizesCanonicalUnicodeAndIgnoresCaseAndOuterWhitespace() {
        assertEquals(listOf("e"), querySites(fixture, SiteQuery(search = "  І\u0308ЖАК  ")).map { it.site.id })
        assertEquals(fixture.size, querySites(fixture, SiteQuery(search = " \t ")).size)
        assertTrue(querySites(fixture, SiteQuery(search = "Неіснуючий")).isEmpty())
    }

    @Test fun decimalCommaPointExponentAndExplicitZeroAreAccepted() {
        mapOf("  +12,5  " to 12.5, "12.5" to 12.5, ".5" to 0.5, ",5" to 0.5,
            "12." to 12.0, "1,2e2" to 120.0, "1E-2" to 0.01, "0" to 0.0, "0.000" to 0.0).forEach { (text, value) ->
            val query = SiteQuery(minPowerKw = text, maxPowerKw = text)
            assertNull(text, query.error)
            assertEquals(value, query.minPower!!, 0.0)
            assertEquals(value, query.maxPower!!, 0.0)
        }
        assertNull(SiteQuery(minPowerKw = " \t ", maxPowerKw = "").error)
    }

    @Test fun nonDecimalOverflowAndNonzeroUnderflowNeverSilentlyChangeTheRange() {
        listOf("-1", "-0", "NaN", "Infinity", "1e309", "1e-4000", "0x1.0p3", "12f", "12d", "1_000",
            "1,2.3", "12 кВт", ".", "1e", "0e999999999999").forEach { bad ->
            for (query in listOf(SiteQuery(minPowerKw = bad), SiteQuery(maxPowerKw = bad))) {
                assertNotNull(bad, query.error)
                assertTrue(bad, querySites(fixture, query).isEmpty())
            }
        }
    }

    @Test fun allFilterCombinationsUseAndAndPreserveTheKnownNumericOrder() {
        val input = DemoSites.sites.map { SiteReading(it, 1.0, 10.0, Instant.EPOCH) }
        val powerOrder = listOf("home-1", "home-2", "office-2", "office-1", "factory-1", "factory-2")
        val all = powerOrder.toSet()
        val types = mapOf(null to all, SiteType.HOME to setOf("home-1", "home-2"),
            SiteType.OFFICE to setOf("office-1", "office-2"), SiteType.INDUSTRY to setOf("factory-1", "factory-2"))
        val statuses = mapOf(null to all, SiteStatus.ACTIVE to setOf("home-1", "office-1", "office-2", "factory-1"),
            SiteStatus.MAINTENANCE to setOf("home-2", "factory-2"))
        val searches = mapOf("" to all, " ОФІС " to setOf("office-1", "office-2"),
            "будинок" to setOf("home-1", "home-2"), "відсутній" to emptySet())
        val ranges = mapOf(("" to "") to all, ("12" to "45") to setOf("home-1", "home-2", "office-1", "office-2"),
            ("120" to "") to setOf("factory-1", "factory-2"), ("" to "12") to setOf("home-1"), ("18" to "18") to setOf("home-2"))
        for ((type, typeIds) in types) for ((status, statusIds) in statuses)
            for ((search, searchIds) in searches) for ((range, rangeIds) in ranges) {
                val allowed = typeIds intersect statusIds intersect searchIds intersect rangeIds
                val query = SiteQuery(search, type, status, range.first, range.second, SiteSort.POWER, SiteSort.NAME)
                assertEquals(query.toString(), powerOrder.filter { it in allowed }, querySites(input.reversed(), query).map { it.site.id })
            }
    }

    @Test fun oneSidedBoundsAreInclusiveAndInvalidOrderIsExplicit() {
        assertEquals(listOf("a"), querySites(fixture, SiteQuery(minPowerKw = "100")).map { it.site.id })
        assertEquals(listOf("c"), querySites(fixture, SiteQuery(maxPowerKw = "3")).map { it.site.id })
        assertEquals("Мінімум перевищує максимум.", SiteQuery(minPowerKw = "20", maxPowerKw = "3").error)
    }
}
