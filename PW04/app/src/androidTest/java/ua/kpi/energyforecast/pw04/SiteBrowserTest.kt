package ua.kpi.energyforecast.pw04

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ua.kpi.energyforecast.pw04.data.*
import ua.kpi.energyforecast.pw04.logic.*
import ua.kpi.energyforecast.pw04.model.*
import ua.kpi.energyforecast.pw04.ui.*
import java.time.Instant

/** Мапа замінена тестовою кнопкою: ці тести не підтверджують рендер Google Maps. */
class SiteBrowserTest {
    @get:Rule val compose = createComposeRule()
    private val readings = DemoSites.sites.map { SiteReading(it, 1.0, 8.0, Instant.EPOCH) }

    @Test fun markerSelectionScrollsAndHighlightsAssociatedCard() {
        compose.setContent {
            var selected by remember { mutableStateOf<String?>(null) }
            MaterialTheme {
                SiteBrowser(ForecastUiState(telemetry = TelemetryState(readings), visible = readings, selectedId = selected),
                    onQuery = {}, onSelect = { selected = it }, onDetails = {}) {
                    Button(onClick = { selected = "factory-2" }) { Text("Тестовий маркер") }
                }
            }
        }
        compose.onNodeWithText("Тестовий маркер").performClick()
        compose.onNodeWithTag("site-factory-2").assertIsDisplayed().assertIsSelected()
    }

    @Test fun cardClickOpensDetailsForTheSelectedId() {
        var selected: String? = null
        var details: String? = null
        compose.setContent { MaterialTheme {
            SiteBrowser(ForecastUiState(visible = readings.take(1)), {}, { selected = it }, { details = it }) { Text("Тестова мапа") }
        } }
        compose.onNodeWithTag("site-home-1").performClick()
        compose.runOnIdle {
            assertEquals("home-1", selected)
            assertEquals("home-1", details)
            details = null
        }
        compose.onNodeWithText("Деталі").performClick()
        compose.runOnIdle { assertEquals("home-1", details) }
    }

    @Test fun mapActionSelectsCardWithoutOpeningDetails() {
        var details: String? = null
        compose.setContent {
            var selected by remember { mutableStateOf<String?>(null) }
            MaterialTheme {
                SiteBrowser(ForecastUiState(visible = readings.take(1), selectedId = selected),
                    {}, { selected = it }, { details = it }) { Text("Тестова мапа") }
            }
        }
        compose.onNodeWithText("На мапі").performClick()
        compose.onNodeWithTag("site-home-1").assertIsSelected()
        compose.runOnIdle { assertEquals(null, details) }
    }

    @Test fun searchUpdatesVisibleCollectionAndShowsEmptyState() {
        compose.setContent {
            var query by remember { mutableStateOf(SiteQuery()) }
            MaterialTheme {
                SiteBrowser(ForecastUiState(telemetry = TelemetryState(readings, StreamPhase.LIVE), query = query, visible = querySites(readings, query)),
                    { query = it }, {}, {}) { Text("Тестова мапа") }
            }
        }
        compose.onNodeWithText("Пошук за назвою").performTextInput("Неіснуючий")
        compose.onNodeWithText("За цими умовами об'єктів немає.").assertIsDisplayed()
        compose.onNodeWithText("Скинути фільтри").performClick()
        compose.onNodeWithTag("site-home-1").assertIsDisplayed()
    }

    @Test fun cardShowsDescriptionPowerStatusForecastAndBothDates() {
        val reading = readings.first()
        compose.setContent { MaterialTheme {
            SiteBrowser(ForecastUiState(telemetry = TelemetryState(readings, StreamPhase.LIVE), visible = listOf(reading)), {}, {}, {}) { Text("Тестова мапа") }
        } }
        compose.onNodeWithTag("site-home-1").assertTextContains(reading.site.description)
            .assertTextContains("Житло • Активний")
            .assertTextContains("Встановлена потужність: 12,00 кВт")
            .assertTextContains("Виміряне навантаження: 1,00 кВт")
            .assertTextContains("Добовий прогноз: 8,00 кВт·год")
            .assertTextContains("Оновлено опис: ${reading.site.updatedAt}")
            .assertTextContains("Час виміру: ${reading.sampledAt}")
    }

    @Test fun emptyDatasetIsDifferentFromNoSearchMatches() {
        compose.setContent { MaterialTheme {
            SiteBrowser(ForecastUiState(telemetry = TelemetryState(phase = StreamPhase.LIVE)), {}, {}, {}) { Text("Тестова мапа") }
        } }
        compose.onNodeWithText("Об'єктів споживання поки немає.").assertIsDisplayed()
        compose.onNodeWithText("За цими умовами об'єктів немає.").assertDoesNotExist()
        compose.onNodeWithText("Скинути фільтри").assertIsDisplayed()
    }

    @Test fun mapVisibilitySurvivesSavedUiRestorationWithoutLosingSelectedId() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            var selected by rememberSaveable { mutableStateOf<String?>(null) }
            MaterialTheme {
                SiteBrowser(ForecastUiState(telemetry = TelemetryState(readings, StreamPhase.LIVE), visible = readings.take(1), selectedId = selected),
                    {}, { selected = it }, {}) { Text("Тестова мапа") }
            }
        }
        compose.onNodeWithText("На мапі").performScrollTo().performClick()
        compose.onNodeWithTag("map-toggle").performClick()
        compose.onNodeWithTag("site-map").assertDoesNotExist()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("site-map").assertDoesNotExist()
        compose.onNodeWithTag("site-home-1").assertIsSelected()
        compose.onNodeWithTag("map-toggle").performClick()
        compose.onNodeWithTag("site-map").assertExists()
    }

    @Test fun shortWideViewportKeepsTheLastCardReachable() {
        compose.setContent { MaterialTheme {
            Box(Modifier.requiredSize(width = 640.dp, height = 300.dp)) {
                SiteBrowser(ForecastUiState(telemetry = TelemetryState(readings, StreamPhase.LIVE), visible = readings), {}, {}, {}) { Text("Тестова мапа") }
            }
        } }
        compose.onNodeWithTag("site-list").performScrollToNode(hasTestTag("site-factory-2"))
        compose.onNodeWithTag("site-factory-2").assertIsDisplayed()
        compose.onNodeWithTag("site-map").assertIsDisplayed()
    }

    @Test fun repeatedSelectionScrollsBackToTheSameCardAfterManualScroll() {
        compose.setContent {
            var revision by remember { mutableStateOf(0L) }
            MaterialTheme {
                SiteBrowser(ForecastUiState(telemetry = TelemetryState(readings, StreamPhase.LIVE), visible = readings,
                    selectedId = "home-1", selectionRevision = revision), {}, {}, {}) {
                    Button(onClick = { revision++ }) { Text("Повторний вибір") }
                }
            }
        }
        compose.onNodeWithTag("site-list").performScrollToNode(hasTestTag("site-factory-2"))
        compose.onNodeWithTag("site-factory-2").assertIsDisplayed()
        compose.onNodeWithText("Повторний вибір").performClick()
        compose.onNodeWithTag("site-home-1").assertIsDisplayed().assertIsSelected()
    }
}
