package ua.kpi.energyforecast.pw04

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import org.junit.Rule
import org.junit.Test

class ActivityLifecycleTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun prepareSelection() {
        compose.onNodeWithText("Об'єкти й мапа").performClick()
        compose.onNodeWithText("Пошук за назвою").performTextInput("Офіс")
        compose.onNodeWithText("Пошук за назвою").performImeAction()
        compose.waitUntil(10000) { compose.onAllNodesWithTag("site-office-2").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("map-toggle").performClick()
        compose.onNodeWithTag("site-list").performScrollToNode(hasTestTag("site-office-1"))
        compose.onNodeWithTag("site-office-1").onChildren().filter(hasText("На мапі")).onFirst().performClick()
    }
    private fun assertRestored() {
        compose.waitUntil(10000) { compose.onAllNodesWithTag("site-office-1").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Пошук за назвою").assertTextContains("Офіс")
        compose.onNodeWithTag("site-map").assertDoesNotExist()
        compose.onNodeWithTag("site-list").performScrollToNode(hasTestTag("site-office-1"))
        compose.onNodeWithTag("site-office-1").assertIsSelected()
    }
    @Test fun stoppedActivityReturnsToSameQuerySelectionAndMapVisibility() {
        prepareSelection()
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        assertRestored()
    }
    @Test fun landscapeThenPortraitKeepsQuerySelectionAndMapVisibility() {
        prepareSelection()
        try {
            compose.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            compose.waitUntil(10000) { compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE }
            assertRestored()
            compose.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
            compose.waitUntil(10000) { compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT }
            assertRestored()
        } finally {
            compose.activityRule.scenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        }
    }
}
