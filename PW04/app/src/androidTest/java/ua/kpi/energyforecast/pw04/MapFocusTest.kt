package ua.kpi.energyforecast.pw04

import androidx.compose.runtime.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ua.kpi.energyforecast.pw04.data.DemoSites
import ua.kpi.energyforecast.pw04.model.*
import ua.kpi.energyforecast.pw04.ui.SiteFocusEffect
import java.time.Instant

/** Перевіряється ефект, використаний камерою EnergyMap, без Google Maps SDK. */
class MapFocusTest {
    @get:Rule val compose = createComposeRule()
    private val readings = DemoSites.sites.map { SiteReading(it, 1.0, 8.0, Instant.EPOCH) }

    @Test fun sameIdRefocusesOnlyOnNewExplicitRevision() {
        var revision by mutableStateOf(1L)
        var data by mutableStateOf(readings)
        val targets = mutableListOf<Coordinates>()
        compose.setContent { SiteFocusEffect(data, "home-1", revision, true) { targets += it } }
        compose.runOnIdle {
            assertEquals(1, targets.size)
            data = readings.reversed().map { it.copy(forecastDailyKwh = 99.0) }
        }
        compose.runOnIdle { assertEquals(1, targets.size); revision++ }
        compose.runOnIdle {
            assertEquals(2, targets.size)
            assertEquals(listOf(readings.first().site.position, readings.first().site.position), targets)
        }
    }

    @Test fun waitsForMapAndUsesOnlyLatestVisibleSelection() {
        var id by mutableStateOf<String?>("home-1")
        var ready by mutableStateOf(false)
        var data by mutableStateOf(readings)
        val targets = mutableListOf<Coordinates>()
        compose.setContent { SiteFocusEffect(data, id, 0, ready) { targets += it } }
        compose.runOnIdle { assertEquals(0, targets.size); id = "office-1" }
        compose.runOnIdle { assertEquals(0, targets.size); ready = true }
        compose.runOnIdle {
            assertEquals(listOf(readings.first { it.site.id == "office-1" }.site.position), targets)
            data = readings.filterNot { it.site.id == "office-1" }
        }
        compose.runOnIdle { assertEquals(1, targets.size); id = null }
        compose.runOnIdle { assertEquals(1, targets.size) }
    }

    @Test fun callbackRecompositionDoesNotRefocusAndNextActionUsesLatestCallback() {
        var revision by mutableStateOf(1L)
        var useSecond by mutableStateOf(false)
        var first = 0
        var second = 0
        compose.setContent { SiteFocusEffect(readings, "home-1", revision, true,
            if (useSecond) ({ second++ }) else ({ first++ })) }
        compose.runOnIdle { assertEquals(1, first); useSecond = true }
        compose.runOnIdle { assertEquals(0, second); revision++ }
        compose.runOnIdle { assertEquals(1, first); assertEquals(1, second) }
    }
}
