package ua.kpi.energyforecast.pw04

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.junit.Rule
import org.junit.Test

class DetailsNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun unknownRouteIdShowsNotFoundAndBackReturnsToCatalog() {
        lateinit var nav: NavHostController
        compose.setContent { MaterialTheme {
            nav = rememberNavController()
            EnergyApp(nav = nav)
        } }
        compose.waitUntil(timeoutMillis = 10000) {
            compose.onAllNodesWithText("Лічильник: дані надходять").fetchSemanticsNodes().isNotEmpty()
        }
        compose.runOnIdle { nav.navigate("sites"); nav.navigate("details/missing-id") }
        compose.onNodeWithText("Об'єкт не знайдено. Поверніться до каталогу.").assertIsDisplayed()
        compose.onNodeWithText("Назад").performClick()
        compose.onNodeWithText("Об'єкти споживання").assertIsDisplayed()
    }
}
