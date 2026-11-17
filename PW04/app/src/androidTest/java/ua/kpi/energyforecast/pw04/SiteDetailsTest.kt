package ua.kpi.energyforecast.pw04

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import ua.kpi.energyforecast.pw04.data.*
import ua.kpi.energyforecast.pw04.model.SiteReading
import ua.kpi.energyforecast.pw04.ui.SiteDetails
import java.time.Instant

class SiteDetailsTest {
    @get:Rule val compose = createComposeRule()
    private val readings = DemoSites.sites.map { SiteReading(it, 1.0, 8.0, Instant.EPOCH) }

    @Test fun detailsResolveTheRequestedIdAndShowAllRequiredFields() {
        val reading = readings.first { it.site.id == "office-1" }
        compose.setContent { MaterialTheme {
            SiteDetails("office-1", TelemetryState(readings, StreamPhase.LIVE), {}, {})
        } }
        compose.onNodeWithText("Офіс Обрій").assertIsDisplayed()
        compose.onNodeWithText("Будинок Дніпро").assertDoesNotExist()
        listOf("Тип: Офіс", "Статус: Активний", reading.site.description,
            "Встановлена потужність: 45,00 кВт", "Режим: 10,00 год/добу",
            "Виміряне навантаження: 1,00 кВт", "Добовий прогноз: 8,00 кВт·год",
            "Оновлено опис: ${reading.site.updatedAt}", "Час виміру: ${reading.sampledAt}",
            "Координати: ${reading.site.position.latitude}, ${reading.site.position.longitude}").forEach { text ->
            compose.onNodeWithTag("details-list").performScrollToNode(hasText(text))
            compose.onNodeWithText(text).assertIsDisplayed()
        }
        compose.onNodeWithText("Назад").assertIsDisplayed()
    }

    @Test fun loadingChangesToTheRequestedObjectWhenTelemetryArrives() {
        var state by mutableStateOf(TelemetryState())
        compose.setContent { MaterialTheme { SiteDetails("office-1", state, {}, {}) } }
        compose.onNodeWithText("Очікуємо дані об'єкта…").assertIsDisplayed()
        compose.onNodeWithText("Об'єкт не знайдено. Поверніться до каталогу.").assertDoesNotExist()
        compose.runOnIdle { state = TelemetryState(readings, StreamPhase.LIVE) }
        compose.onNodeWithText("Офіс Обрій").assertIsDisplayed()
        compose.onNodeWithTag("details-empty").assertDoesNotExist()
    }

    @Test fun unknownIdHasExplicitNotFoundAndWorkingBackAction() {
        var back = 0
        compose.setContent { MaterialTheme {
            SiteDetails("missing-id", TelemetryState(readings, StreamPhase.LIVE), { back++ }, {})
        } }
        compose.onNodeWithText("Об'єкт не знайдено. Поверніться до каталогу.").assertIsDisplayed()
        compose.onNodeWithText("Відновити").assertDoesNotExist()
        compose.onNodeWithText("Назад").performClick()
        compose.runOnIdle { assertEquals(1, back) }
    }

    @Test fun unavailableDataOffersRestartAndDoesNotClaimObjectIsMissing() {
        var restart = 0
        compose.setContent { MaterialTheme {
            SiteDetails("home-1", TelemetryState(phase = StreamPhase.FAILED,
                meterStatus = SourceStatus(StreamPhase.FAILED, 3, 3, StreamIssue.NO_DATA)), {}, { restart++ })
        } }
        compose.onNodeWithText("Не вдалося отримати дані об'єкта. Спробуйте відновити збір.").assertIsDisplayed()
        compose.onNodeWithText("Об'єкт не знайдено. Поверніться до каталогу.").assertDoesNotExist()
        compose.onNodeWithText("Відновити").performClick()
        compose.runOnIdle { assertEquals(1, restart) }
    }

    @Test fun retainedDetailsRemainIdentifiedAndMarkedStaleDuringFailure() {
        compose.setContent { MaterialTheme {
            SiteDetails("home-1", TelemetryState(readings, StreamPhase.RETRYING,
                meterStatus = SourceStatus(StreamPhase.RETRYING, 1, 3, StreamIssue.IO),
                forecastStatus = SourceStatus(StreamPhase.LIVE)), {}, {})
        } }
        compose.onNodeWithText("Останні дані застарілі").assertIsDisplayed()
        compose.onNodeWithText("Будинок Дніпро").assertIsDisplayed()
        compose.onNodeWithTag("details-empty").assertDoesNotExist()
    }
}
