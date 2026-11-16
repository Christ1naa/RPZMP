package ua.kpi.energyforecast

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import ua.kpi.energyforecast.ui.screens.MainMenuScreen

class RoleUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun adminMenuAppearsOnlyForAdminAndDisappearsAfterRoleChange() {
        val admin = mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                MainMenuScreen(null, {}, {}, {}, {}, {}, admin.value, {})
            }
        }
        compose.onNodeWithText("Відкрити статистику").assertDoesNotExist()
        compose.runOnIdle { admin.value = true }
        compose.onNodeWithText("Відкрити статистику").performScrollTo().assertIsDisplayed()
        compose.runOnIdle { admin.value = false }
        compose.onNodeWithText("Відкрити статистику").assertDoesNotExist()
        compose.onNodeWithText("Відкрити каталог").performScrollTo().assertIsDisplayed()
    }
}
