package ua.kpi.energyforecast.pw04

import androidx.compose.material3.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ua.kpi.energyforecast.pw04.data.*
import ua.kpi.energyforecast.pw04.model.SiteReading
import ua.kpi.energyforecast.pw04.ui.*
import java.time.Instant

class StreamStatusTest {
    @get:Rule val compose = createComposeRule()

    @Test fun missingSourceIsNotPresentedAsAnEmptySearchAndCanRestart() {
        var restarted = 0
        val state = TelemetryState(phase = StreamPhase.FAILED,
            meterStatus = SourceStatus(StreamPhase.FAILED, 3, 3, StreamIssue.NO_DATA),
            forecastStatus = SourceStatus(StreamPhase.LIVE))
        compose.setContent { MaterialTheme {
            SiteBrowser(ForecastUiState(telemetry = state), {}, {}, {}, onRestart = { restarted++ }) { Text("Тестова мапа") }
        } }
        compose.onNodeWithText("За цими умовами об'єктів немає.").assertDoesNotExist()
        compose.onNodeWithText("Лічильник: Немає валідних даних • збір зупинено").assertIsDisplayed()
        compose.onNodeWithText("Відновити").performClick()
        compose.runOnIdle { assertEquals(1, restarted) }
    }

    @Test fun forecastRetryIdentifiesSourceAndMarksLastSnapshotStale() {
        val reading = SiteReading(DemoSites.sites.first(), 1.0, 8.0, Instant.EPOCH)
        val state = TelemetryState(listOf(reading), StreamPhase.RETRYING,
            meterStatus = SourceStatus(StreamPhase.LIVE),
            forecastStatus = SourceStatus(StreamPhase.RETRYING, 1, 3, StreamIssue.IO))
        compose.setContent { MaterialTheme { StreamStatus(state) } }
        compose.onNodeWithText("Лічильник: дані надходять").assertIsDisplayed()
        compose.onNodeWithText("Модель: Збій джерела • повтор 1/3").assertIsDisplayed()
        compose.onNodeWithText("Останні дані застарілі").assertIsDisplayed()
    }

    @Test fun waitingMonitorDoesNotDisplayZeroConsumptionAsMeasuredData() {
        compose.setContent { MaterialTheme { MonitorScreen(TelemetryState(), {}, {}, {}) } }
        compose.onNodeWithText("Немає даних").assertIsDisplayed()
        compose.onNodeWithText("0,00 кВт·год").assertDoesNotExist()
    }
}
