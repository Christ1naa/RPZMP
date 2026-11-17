package ua.kpi.energyforecast.pw04

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import ua.kpi.energyforecast.pw04.logic.SiteSort

class CompleteEvidenceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun scroll(text: String) = compose.onNodeWithTag("filter-list").performScrollToNode(hasText(text))
    private fun sitesWithoutLocationMap() {
        compose.onNodeWithText("Об'єкти й мапа").performClick()
        compose.onNodeWithTag("map-toggle").performClick()
    }
    @Test fun retryRangeSortingAndOrientationScreens() {
        compose.waitUntil(10000) { compose.onAllNodesWithText("Лічильник: дані надходять").fetchSemanticsNodes().isNotEmpty() }
        for ((button, source, name) in listOf(Triple("Збій лічильника", "Лічильник", "meter"), Triple("Збій моделі", "Модель", "model"))) {
            compose.onNodeWithText(button).performScrollTo().performClick()
            compose.onNodeWithTag("stream-status").performScrollTo()
            compose.waitUntil(10000) { compose.onAllNodesWithText("$source: Збій джерела • повтор 1/3").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Останні дані застарілі").assertIsDisplayed()
            captureVerificationScreen("20-$name-retrying")
            compose.waitUntil(10000) {
                compose.onAllNodesWithText("$source: дані надходять").fetchSemanticsNodes().isNotEmpty() &&
                    compose.onAllNodesWithText("Останні дані застарілі").fetchSemanticsNodes().isEmpty()
            }
            captureVerificationScreen("21-$name-recovered")
        }
        sitesWithoutLocationMap()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("site-home-1").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Фільтри").performClick()
        compose.onNodeWithTag("filter-list").performScrollToNode(hasTestTag("min-power"))
        compose.onNodeWithTag("min-power").performTextReplacement("50")
        compose.onNodeWithTag("filter-list").performScrollToNode(hasTestTag("max-power"))
        compose.onNodeWithTag("max-power").performTextReplacement("10")
        compose.onNodeWithTag("max-power").performImeAction()
        compose.onNodeWithTag("filter-list").performScrollToNode(hasTestTag("filter-error"))
        compose.onNodeWithTag("filter-error").assertIsDisplayed()
        captureVerificationScreen("22-invalid-range")
        scroll("Скинути"); compose.onNodeWithText("Скинути").performClick()
        scroll("Готово"); compose.onNodeWithText("Готово").performClick()
        var current = SiteSort.NAME
        var descending = false
        for (sort in SiteSort.entries) for (desc in listOf(false, true)) {
            compose.onNodeWithText("Фільтри").performClick()
            scroll("Перший критерій: ${current.label}")
            compose.onNodeWithText("Перший критерій: ${current.label}").performClick()
            compose.onNodeWithText(sort.label).performClick(); current = sort
            compose.onNodeWithTag("filter-list").performScrollToNode(hasTestTag("sort-descending"))
            if (descending != desc) { compose.onNodeWithTag("sort-descending").performClick(); descending = desc }
            scroll("Готово"); compose.onNodeWithText("Готово").performClick()
            compose.onNodeWithTag("query-summary").assertTextContains(sort.label, substring = true)
            compose.onNodeWithTag("query-summary").assertTextContains(if (desc) "↓" else "↑", substring = true)
            captureVerificationScreen("23-sort-${sort.name.lowercase()}-${if (desc) "desc" else "asc"}")
        }
        try {
            compose.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            compose.waitUntil(10000) { compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
            compose.onNodeWithTag("site-list").performScrollToNode(hasTestTag("site-factory-2"))
            android.os.SystemClock.sleep(1500)
            captureVerificationScreen("24-landscape")
            compose.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
            compose.waitUntil(10000) { compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT }
            android.os.SystemClock.sleep(1500)
            captureVerificationScreen("25-portrait-restored")
        } finally {
            compose.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        }
    }
    @NeedsHostSetup @Test fun actualSystemLargeFontScreens() {
        requireHostSetup()
        assertTrue(compose.activity.resources.configuration.fontScale >= 1.5f)
        sitesWithoutLocationMap()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("site-home-1").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("site-list").performScrollToNode(hasTestTag("site-factory-2"))
        compose.onNodeWithTag("site-factory-2").assertIsDisplayed()
        captureVerificationScreen("26-system-large-font")
        compose.onNodeWithText("Фільтри").performClick()
        scroll("Скинути"); compose.onNodeWithText("Скинути").assertIsDisplayed()
        captureVerificationScreen("27-large-font-filter-reset")
        scroll("Готово"); compose.onNodeWithText("Готово").assertIsDisplayed()
        captureVerificationScreen("28-large-font-filter-done")
        compose.onNodeWithText("Готово").performClick()
    }
}
