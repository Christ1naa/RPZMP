package ua.kpi.energyforecast.pw04

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

/** Виконується лише на власному AVD з тимчасово вимкненим GMS, не на телефоні. */
class ServiceUnavailableTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @NeedsHostSetup @Test fun unavailablePlayServicesDoesNotBlockCatalogue() {
        requireHostSetup()
        compose.onNodeWithText("Об'єкти й мапа").performClick()
        compose.onNodeWithText("Google Play services недоступні. Перевірте пристрій та їх оновлення.").assertIsDisplayed()
        compose.onNodeWithTag("google-map").assertDoesNotExist()
        captureVerificationScreen("60-play-services-unavailable")
        compose.onNodeWithTag("map-toggle").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("site-home-1").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("site-home-1").performClick()
        compose.onNodeWithText("Будинок Дніпро").assertIsDisplayed()
        captureVerificationScreen("61-services-unavailable-details-work")
    }
}
