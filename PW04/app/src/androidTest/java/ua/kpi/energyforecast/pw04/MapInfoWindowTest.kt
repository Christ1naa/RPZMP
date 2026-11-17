package ua.kpi.energyforecast.pw04

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import ua.kpi.energyforecast.pw04.data.DemoSites
import ua.kpi.energyforecast.pw04.model.SiteReading
import ua.kpi.energyforecast.pw04.ui.SiteInfoWindow
import ua.kpi.energyforecast.pw04.ui.siteMarkers
import java.time.Instant

class MapInfoWindowTest {
    @Before fun keepScreenAwake() = keepMapsTestScreenAwake()
    @get:Rule val compose = createComposeRule()
    @Test fun descriptionAndMeasurementsWrapWithoutEllipsis() {
        val reading = SiteReading(DemoSites.sites.first { it.id == "office-2" }, 1.0, 8.0, Instant.EPOCH)
        val marker = siteMarkers(listOf(reading), reading.site.id).single()
        compose.setContent { MaterialTheme { SiteInfoWindow(marker) } }
        compose.onNodeWithText(marker.title).assertIsDisplayed()
        val details = compose.onNodeWithTag("marker-info-details")
        details.assertTextContains(marker.snippet)
        val layout = mutableListOf<TextLayoutResult>()
        details.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layout) }
        assertTrue(layout.isNotEmpty())
        assertFalse(layout.single().hasVisualOverflow)
        assertTrue(layout.single().lineCount >= 3)
        for (line in 0 until layout.single().lineCount) assertFalse(layout.single().isLineEllipsized(line))
    }
}
