package ua.kpi.energyforecast.pw04

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import ua.kpi.energyforecast.pw04.logic.SiteSort

/** Реальна Activity, SavedStateHandle factory, навігація та симулятор Flow. Ключ мапи не потрібен. */
class AppSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun launchesLoadsSitesAndOpensDetails() {
        compose.onNodeWithText("EnergyForecast • PW04").assertIsDisplayed()
        compose.waitUntil(timeoutMillis = 10000) {
            compose.onAllNodesWithText("Будинок Дніпро").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Об'єкти й мапа").performClick()
        compose.onAllNodesWithText("На мапі")[0].performScrollTo().performClick()
        compose.onNodeWithTag("site-home-1").assertIsSelected()
        compose.onNodeWithTag("site-home-1").assertIsDisplayed().performClick()
        compose.onNodeWithText("Будинок Дніпро").assertIsDisplayed()
        compose.onNodeWithTag("details-list").performScrollToNode(hasText("Об'єкт і координатна прив'язка вигадані."))
        compose.onNodeWithText("Об'єкт і координатна прив'язка вигадані.").assertIsDisplayed()
        compose.onNodeWithText("Назад").performClick()
        compose.onNodeWithText("Об'єкти споживання").assertIsDisplayed()
    }

    @Test fun filteredSelectionAndDetailsRouteSurviveActivityRecreation() {
        compose.onNodeWithText("Об'єкти й мапа").performClick()
        compose.onNodeWithText("Пошук за назвою").performTextInput("Офіс")
        compose.waitUntil(timeoutMillis = 10000) {
            compose.onAllNodesWithTag("site-office-2").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("site-list").performScrollToNode(hasTestTag("site-office-1"))
        compose.onNodeWithTag("site-office-1").performClick()
        compose.onNodeWithText("Офіс Обрій").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.waitUntil(timeoutMillis = 10000) {
            compose.onAllNodesWithText("Офіс Обрій").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("site-details").assertIsDisplayed()
        compose.onNodeWithText("Назад").performClick()
        compose.onNodeWithText("Пошук за назвою").assertTextContains("Офіс")
        compose.onNodeWithTag("site-list").performScrollToNode(hasTestTag("site-office-1"))
        compose.onNodeWithTag("site-office-1").assertIsSelected()
        compose.onNodeWithTag("site-home-1").assertDoesNotExist()
    }

    @Test fun completeFilterPanelIsWiredToViewModelAndRestoredAfterRecreation() {
        fun scroll(text: String) {
            compose.onNodeWithTag("filter-list").performScrollToNode(hasText(text))
        }
        fun choose(label: String, current: String, value: String) {
            scroll("$label: $current")
            compose.onNodeWithText("$label: $current").performClick()
            compose.onNodeWithText(value).performClick()
        }
        compose.onNodeWithText("Об'єкти й мапа").performClick()
        compose.waitUntil(timeoutMillis = 10000) {
            compose.onAllNodesWithTag("site-home-1").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Фільтри").performClick()
        choose("Тип", "Усі", "Офіс")
        choose("Статус", "Усі", "Активний")
        compose.onNodeWithTag("filter-list").performScrollToNode(hasTestTag("min-power"))
        compose.onNodeWithTag("min-power").performTextReplacement("30,0")
        compose.onNodeWithTag("filter-list").performScrollToNode(hasTestTag("max-power"))
        compose.onNodeWithTag("max-power").performTextReplacement("50")
        compose.onNodeWithTag("max-power").performImeAction()
        choose("Перший критерій", SiteSort.NAME.label, SiteSort.DATE.label)
        compose.onNodeWithTag("filter-list").performScrollToNode(hasTestTag("sort-descending"))
        compose.onNodeWithTag("sort-descending").performClick()
        scroll("Готово")
        compose.onNodeWithText("Готово").performClick()
        compose.onNodeWithTag("query-summary").assertTextContains("Знайдено: 1 з 6", substring = true)
        compose.onNodeWithTag("site-office-1").assertIsDisplayed()
        compose.onNodeWithTag("site-office-2").assertDoesNotExist()
        compose.activityRule.scenario.recreate()
        compose.waitUntil(timeoutMillis = 10000) {
            compose.onAllNodesWithTag("site-office-1").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("query-summary").assertTextContains("Знайдено: 1 з 6", substring = true)
        compose.onNodeWithText("Фільтри").performClick()
        compose.onNodeWithText("Тип: Офіс").assertIsDisplayed()
        scroll("Статус: Активний")
        compose.onNodeWithText("Статус: Активний").assertIsDisplayed()
        compose.onNodeWithTag("filter-list").performScrollToNode(hasTestTag("min-power"))
        compose.onNodeWithTag("min-power").assertTextContains("30,0")
        compose.onNodeWithTag("filter-list").performScrollToNode(hasTestTag("max-power"))
        compose.onNodeWithTag("max-power").assertTextContains("50")
        scroll("Перший критерій: ${SiteSort.DATE.label}")
        compose.onNodeWithText("Перший критерій: ${SiteSort.DATE.label}").assertIsDisplayed()
        compose.onNodeWithTag("filter-list").performScrollToNode(hasTestTag("sort-descending"))
        compose.onNodeWithTag("sort-descending").assertIsOn()
        scroll("Скинути")
        compose.onNodeWithText("Скинути").performClick()
        scroll("Готово")
        compose.onNodeWithText("Готово").performClick()
        compose.onNodeWithTag("query-summary").assertTextContains("Знайдено: 6 з 6", substring = true)
    }

    @Test fun bothDemoSourcesShowFailureAndRecoverInRealActivity() {
        compose.waitUntil(timeoutMillis = 10000) {
            compose.onAllNodesWithText("Лічильник: дані надходять").fetchSemanticsNodes().isNotEmpty()
        }
        listOf("Збій лічильника" to "Лічильник", "Збій моделі" to "Модель").forEach { (button, source) ->
            compose.onNodeWithText(button).performScrollTo().performClick()
            compose.onNodeWithTag("stream-status").performScrollTo()
            compose.waitUntil(timeoutMillis = 10000) {
                compose.onAllNodesWithText("$source: Збій джерела • повтор 1/3").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Останні дані застарілі").assertIsDisplayed()
            compose.waitUntil(timeoutMillis = 10000) {
                compose.onAllNodesWithText("$source: дані надходять").fetchSemanticsNodes().isNotEmpty() &&
                    compose.onAllNodesWithText("Останні дані застарілі").fetchSemanticsNodes().isEmpty()
            }
        }
    }
}
