package ua.kpi.energyforecast.pw04

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

/** Реальна Activity, SavedStateHandle factory, навігація та симулятор Flow. Ключ мапи не потрібен. */
class AppSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun launchesLoadsSitesAndOpensDetails() {
        compose.onNodeWithText("EnergyForecast • PW04").assertIsDisplayed()
        compose.waitUntil(timeoutMillis = 10000) {
            compose.onAllNodesWithText("Будинок Дніпро").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Об'єкти й мапа").performClick()
        compose.onNodeWithTag("site-home-1").assertIsDisplayed().performClick()
        compose.onAllNodesWithText("Деталі")[0].performClick()
        compose.onNodeWithText("Будинок Дніпро").assertIsDisplayed()
        compose.onNodeWithText("Назад").performClick()
        compose.onNodeWithText("Об'єкти споживання").assertIsDisplayed()
    }
}
