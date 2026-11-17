package ua.kpi.energyforecast.pw04

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test

/** Host встановлює окремий APK із навмисно неправильним ключем. */
class MapFailureEvidenceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @NeedsHostSetup @Test fun failedMapCanRetryAndCatalogueRemainsUsable() {
        requireHostSetup()
        val prefix = InstrumentationRegistry.getArguments().getString("evidencePrefix", "invalid-key")
        compose.onNodeWithText("Об'єкти й мапа").performClick()
        try {
            // LaunchedEffect delay uses the Compose test scheduler, not wall-clock time.
            compose.mainClock.advanceTimeBy(20_100)
            compose.waitUntil(45000) { compose.onAllNodesWithText("Повторити мапу").fetchSemanticsNodes().isNotEmpty() }
        } catch (failure: Throwable) {
            captureVerificationScreen("40-$prefix-unexpected-state")
            throw failure
        }
        compose.onNodeWithText("Мапа не завантажилася. Перевірте ключ, мережу та доступ до Maps SDK.").assertIsDisplayed()
        captureVerificationScreen("40-$prefix-map-failed")
        compose.onNodeWithText("Повторити мапу").performClick()
        compose.onNodeWithText("Завантаження мапи… Перевірте мережу та ключ Google Maps.").assertIsDisplayed()
        captureVerificationScreen("41-$prefix-map-retrying")
        // LaunchedEffect delay uses the Compose test scheduler, not wall-clock time.
        compose.mainClock.advanceTimeBy(20_100)
        compose.waitUntil(45000) { compose.onAllNodesWithText("Повторити мапу").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("map-toggle").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("site-home-1").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("site-home-1").performClick()
        compose.onNodeWithText("Будинок Дніпро").assertIsDisplayed()
        captureVerificationScreen("42-$prefix-catalogue-still-usable")
    }
}
