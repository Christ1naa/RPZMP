package ua.kpi.energyforecast.pw04

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Справжні кадри AVD без Maps. Файли забираються до видалення тестового пакета. */
class EvidenceCaptureTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        val directory = File(instrumentation.targetContext.filesDir, "pw04-evidence").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
    }
    private fun scroll(text: String) = compose.onNodeWithTag("filter-list").performScrollToNode(hasText(text))
    private fun choose(label: String, value: String) {
        scroll("$label: Усі")
        compose.onNodeWithText("$label: Усі").performClick()
        compose.onNodeWithText(value).performClick()
    }
    @NeedsNoMapsKey @Test fun captureVerifiedNoKeyScreens() {
        assumeFalse("Докази цього сценарію призначені для збірки без Maps-ключа", BuildConfig.MAPS_CONFIGURED)
        compose.waitUntil(10000) { compose.onAllNodesWithText("Будинок Дніпро").fetchSemanticsNodes().isNotEmpty() }
        capture("01-monitor-live")
        compose.onNodeWithText("Об'єкти й мапа").performClick()
        compose.onNodeWithText("Для Google Maps додайте MAPS_API_KEY у local.properties та перебудуйте застосунок.")
            .performScrollTo().assertIsDisplayed()
        capture("02-catalog-no-key")
        compose.onNodeWithTag("map-toggle").performClick()
        compose.onNodeWithText("Фільтри").performClick()
        choose("Тип", "Офіс")
        choose("Статус", "Активний")
        compose.onNodeWithTag("filter-list").performScrollToNode(hasTestTag("min-power"))
        compose.onNodeWithTag("min-power").performTextReplacement("30")
        compose.onNodeWithTag("filter-list").performScrollToNode(hasTestTag("max-power"))
        compose.onNodeWithTag("max-power").performTextReplacement("50")
        compose.onNodeWithTag("max-power").performImeAction()
        scroll("Фільтрація та порядок")
        capture("03-filter-panel")
        scroll("Готово"); compose.onNodeWithText("Готово").performClick()
        compose.onNodeWithTag("query-summary").assertTextContains("Знайдено: 1 з 6", substring = true)
        compose.onNodeWithTag("site-office-1").assertIsDisplayed()
        capture("04-filtered-office")
        compose.onNodeWithTag("site-office-1").performClick()
        compose.onNodeWithText("Офіс Обрій").assertIsDisplayed()
        capture("05-details-top")
        compose.onNodeWithTag("details-list").performScrollToNode(hasText("Об'єкт і координатна прив'язка вигадані."))
        capture("06-details-bottom")
        compose.onNodeWithText("Назад").performClick()
        compose.onNodeWithText("Пошук за назвою").performTextInput("Неіснуючий")
        compose.onNodeWithText("За цими умовами об'єктів немає.").assertIsDisplayed()
        capture("07-empty-search")
    }
}
