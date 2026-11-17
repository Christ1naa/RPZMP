package ua.kpi.energyforecast.pw04

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.Assume.assumeFalse
import ua.kpi.energyforecast.pw04.data.*
import ua.kpi.energyforecast.pw04.logic.*
import ua.kpi.energyforecast.pw04.model.*
import ua.kpi.energyforecast.pw04.ui.*
import java.time.Instant

/** Синтетичний viewport/fontScale; не ручна перевірка вигляду Maps. */
class ResponsiveUiTest {
    @get:Rule val compose = createComposeRule()
    private val readings = DemoSites.sites.map { SiteReading(it, 1.0, 8.0, Instant.EPOCH) }
    @Composable private fun Compact(content: @Composable () -> Unit) {
        val density = LocalDensity.current.density
        CompositionLocalProvider(LocalDensity provides Density(density, 2f)) {
            MaterialTheme { Box(Modifier.requiredSize(320.dp, 480.dp)) { content() } }
        }
    }
    @Test fun compactLargeFontKeepsMapToggleAndLastCardActionsReachable() {
        compose.setContent { Compact {
            SiteBrowser(ForecastUiState(telemetry = TelemetryState(readings, StreamPhase.LIVE), visible = readings), {}, {}, {}) {
                Text("Тестовий слот мапи")
            }
        } }
        compose.onNodeWithTag("map-toggle").assertIsDisplayed().performClick()
        compose.onNodeWithTag("site-map").assertDoesNotExist()
        compose.onNodeWithText("Пошук за назвою").assertIsDisplayed()
        compose.onNodeWithTag("site-list").performScrollToNode(hasTestTag("site-factory-2"))
        compose.onNodeWithTag("site-factory-2").assertIsDisplayed()
        compose.onAllNodesWithText("Деталі").onLast().performScrollTo().assertIsDisplayed().performClick()
    }
    @Test fun compactLargeFontDetailsCanReachCoordinatesAndDisclaimer() {
        compose.setContent { Compact { SiteDetails("factory-2", TelemetryState(readings, StreamPhase.LIVE), {}, {}) } }
        compose.onNodeWithText("Назад").assertIsDisplayed()
        compose.onNodeWithTag("details-list").performScrollToNode(hasText("Об'єкт і координатна прив'язка вигадані."))
        compose.onNodeWithText("Об'єкт і координатна прив'язка вигадані.").assertIsDisplayed()
        compose.onNodeWithTag("details-list").performScrollToNode(hasText("Координати:", substring = true))
        compose.onNodeWithText("Координати:", substring = true).assertIsDisplayed()
    }
    @Test fun largeFontFilterSheetCanReachResetAndDone() {
        compose.setContent { Compact {
            var query by remember { mutableStateOf(SiteQuery()) }
            SiteBrowser(ForecastUiState(query = query, visible = readings), { query = it }, {}, {}) { Text("Тестовий слот") }
        } }
        compose.onNodeWithText("Фільтри").assertIsDisplayed().performClick()
        compose.onNodeWithTag("filter-list").performScrollToNode(hasText("Скинути"))
        compose.onNodeWithText("Скинути").assertIsDisplayed().performClick()
        compose.onNodeWithTag("filter-list").performScrollToNode(hasText("Готово"))
        compose.onNodeWithText("Готово").assertIsDisplayed().performClick()
        compose.onNodeWithTag("filter-list").assertDoesNotExist()
    }
    @NeedsNoMapsKey @Test fun shortMapFallbackCanScrollToSettingsAndConfiguration() {
        assumeFalse(BuildConfig.MAPS_CONFIGURED)
        compose.setContent { Compact {
            Box(Modifier.requiredSize(320.dp, 180.dp)) { SiteMapPanel(emptyList(), null, {}) }
        } }
        compose.onNodeWithText("Дозволи").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Для Google Maps додайте MAPS_API_KEY у local.properties та перебудуйте застосунок.")
            .performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("google-map").assertDoesNotExist()
    }
}
