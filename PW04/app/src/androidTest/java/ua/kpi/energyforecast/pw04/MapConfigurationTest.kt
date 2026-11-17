package ua.kpi.energyforecast.pw04

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import ua.kpi.energyforecast.pw04.ui.SiteMapPanel

class MapConfigurationTest {
    @get:Rule val compose = createComposeRule()

    @NeedsNoMapsKey @Test fun missingKeyExplainsConfigurationAndDoesNotPretendMapLoaded() {
        assumeFalse("Цей сценарій призначений лише для збірки без ключа", BuildConfig.MAPS_CONFIGURED)
        compose.setContent { MaterialTheme { SiteMapPanel(emptyList(), null, {}) } }
        compose.onNodeWithText("Для Google Maps додайте MAPS_API_KEY у local.properties та перебудуйте застосунок.").assertIsDisplayed()
        compose.onNodeWithTag("google-map").assertDoesNotExist()
        compose.onNodeWithTag("map-load-status").assertDoesNotExist()
    }
}
