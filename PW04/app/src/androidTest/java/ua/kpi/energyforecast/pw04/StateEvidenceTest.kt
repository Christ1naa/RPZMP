package ua.kpi.energyforecast.pw04

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import ua.kpi.energyforecast.pw04.data.*
import ua.kpi.energyforecast.pw04.model.SiteReading
import ua.kpi.energyforecast.pw04.ui.*
import java.time.Instant

/** Оригінальні production-компоненти з явно позначеними контрольованими UI-станами. */
class StateEvidenceTest {
    @get:Rule val compose = createComposeRule()
    @NeedsHostSetup @Test fun controlledWaitingFailedUnknownAndEmptyStates() {
        requireHostSetup()
        val readings = listOf(SiteReading(DemoSites.sites.first(), 1.0, 8.0, Instant.EPOCH))
        val failed = TelemetryState(readings, StreamPhase.FAILED,
            meterStatus = SourceStatus(StreamPhase.FAILED, 3, 3, StreamIssue.IO),
            forecastStatus = SourceStatus(StreamPhase.LIVE))
        var state by mutableIntStateOf(0)
        compose.setContent { MaterialTheme {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Text("Контрольований UI-стан для звіту • тестові дані", style = MaterialTheme.typography.labelSmall)
                Box(Modifier.weight(1f)) {
                    when (state) {
                        0 -> MonitorScreen(TelemetryState(), {}, {}, {})
                        1 -> MonitorScreen(failed, {}, {}, {})
                        2 -> SiteDetails("unknown-id", TelemetryState(readings, StreamPhase.LIVE), {}, {})
                        else -> SiteBrowser(ForecastUiState(telemetry = TelemetryState(phase = StreamPhase.LIVE)), {}, {}, {}) {}
                    }
                }
            }
        } }
        compose.onNodeWithText("Немає даних").assertIsDisplayed()
        compose.onNodeWithText("0,00 кВт·год").assertDoesNotExist()
        captureVerificationScreen("80-controlled-waiting")
        compose.runOnIdle { state = 1 }
        compose.onNodeWithText("Автовідновлення зупинено. Натисніть «Відновити».").assertIsDisplayed()
        compose.onNodeWithText("Останні дані застарілі").assertIsDisplayed()
        captureVerificationScreen("81-controlled-failed")
        compose.runOnIdle { state = 2 }
        compose.onNodeWithText("Об'єкт не знайдено. Поверніться до каталогу.").assertIsDisplayed()
        captureVerificationScreen("82-controlled-unknown-id")
        compose.runOnIdle { state = 3 }
        compose.onNodeWithText("Об'єктів споживання поки немає.").assertIsDisplayed()
        captureVerificationScreen("83-controlled-empty-dataset")
    }
}
