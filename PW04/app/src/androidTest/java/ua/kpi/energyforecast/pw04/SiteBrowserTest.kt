package ua.kpi.energyforecast.pw04

import androidx.compose.material3.*
import androidx.compose.runtime.*
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

    @Test fun cardSelectionAndDetailsDispatchTheSameId() {
        var selected: String? = null
        var details: String? = null
        compose.setContent { MaterialTheme {
            SiteBrowser(ForecastUiState(visible = readings.take(1)), {}, { selected = it }, { details = it }) { Text("Тестова мапа") }
        } }
        compose.onNodeWithTag("site-home-1").performClick()
        compose.runOnIdle { assertEquals("home-1", selected) }
        compose.onNodeWithText("Деталі").performClick()
        compose.runOnIdle { assertEquals("home-1", details) }
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
}
