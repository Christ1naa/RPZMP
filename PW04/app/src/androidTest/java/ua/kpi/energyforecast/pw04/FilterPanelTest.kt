package ua.kpi.energyforecast.pw04

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ua.kpi.energyforecast.pw04.data.*
import ua.kpi.energyforecast.pw04.logic.*
import ua.kpi.energyforecast.pw04.model.*
import ua.kpi.energyforecast.pw04.ui.*
import java.time.Instant

/** Тестовий слот мапи показує ID тієї самої visible-колекції; це не перевірка Google Maps. */
class FilterPanelTest {
    @get:Rule val compose = createComposeRule()
    private val forecasts = listOf(80.0, 120.0, 400.0, 0.0, 60.0, 0.0)
    private val readings = DemoSites.sites.mapIndexed { i, site -> SiteReading(site, 1.0, forecasts[i], Instant.EPOCH) }
    private var query by mutableStateOf(SiteQuery())

    private fun showBrowser() {
        compose.setContent { MaterialTheme {
            val visible = querySites(readings, query)
            SiteBrowser(ForecastUiState(TelemetryState(readings, StreamPhase.LIVE), query, visible),
                { query = it }, {}, {}) {
                Text(visible.joinToString(",") { it.site.id }, Modifier.testTag("map-ids"))
            }
        } }
    }

    private fun scrollFilter(text: String) {
        compose.onNodeWithTag("filter-list").performScrollToNode(hasText(text))
    }

    private fun choose(label: String, current: String, option: String) {
        scrollFilter("$label: $current")
        compose.onNodeWithText("$label: $current").performClick()
        compose.onNodeWithText(option).performClick()
    }

    private fun bound(tag: String, text: String) {
        compose.onNodeWithTag("filter-list").performScrollToNode(hasTestTag(tag))
        compose.onNodeWithTag(tag).performTextReplacement(text)
        if (tag == "max-power") compose.onNodeWithTag(tag).performImeAction()
    }

    private fun closeFilters() {
        scrollFilter("Готово")
        compose.onNodeWithText("Готово").performClick()
    }

    @Test fun searchTypeStatusAndCommaBoundsProduceOneObjectInListAndMapSlot() {
        showBrowser()
        compose.onNodeWithText("Пошук за назвою").performTextInput(" ОФІС ")
        compose.onNodeWithText("Фільтри").performClick()
        choose("Тип", "Усі", "Офіс")
        choose("Статус", "Усі", "Активний")
        bound("min-power", "30,0")
        bound("max-power", "50")
        closeFilters()
        compose.onNodeWithTag("query-summary").assertTextContains("Знайдено: 1 з 6", substring = true)
        compose.onNodeWithTag("map-ids").assertTextEquals("office-1")
        compose.onNodeWithTag("site-office-1").assertIsDisplayed()
        compose.onNodeWithTag("site-office-2").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(SiteType.OFFICE, query.type)
            assertEquals(SiteStatus.ACTIVE, query.status)
            assertEquals(30.0, query.minPower!!, 0.0)
        }
    }

    @Test fun invalidRangeShowsErrorAndResetRestoresEveryQueryField() {
        query = SiteQuery(search = "Офіс", type = SiteType.OFFICE, status = SiteStatus.ACTIVE,
            primary = SiteSort.FORECAST, secondary = SiteSort.DATE, descending = true)
        showBrowser()
        compose.onNodeWithText("Фільтри").performClick()
        bound("min-power", "50")
        bound("max-power", "10")
        compose.onNodeWithTag("filter-list").performScrollToNode(hasTestTag("filter-error"))
        compose.onNodeWithTag("filter-error").assertTextEquals("Мінімум перевищує максимум.")
        closeFilters()
        compose.onNodeWithTag("query-error").assertIsDisplayed()
        compose.onNodeWithText("За цими умовами об'єктів немає.").assertDoesNotExist()
        compose.onNodeWithTag("site-list").performScrollToNode(hasText("Скинути фільтри"))
        compose.onNodeWithText("Скинути фільтри").performClick()
        compose.onNodeWithTag("query-error").assertDoesNotExist()
        compose.onNodeWithTag("query-summary").assertTextContains("Знайдено: 6 з 6", substring = true)
        compose.runOnIdle { assertEquals(SiteQuery(), query) }
    }

    @Test fun everyPrimaryChoiceAndBothDirectionsReachTheListAndMapSlot() {
        val expected = mapOf(
            SiteSort.NAME to listOf("home-1", "home-2", "factory-1", "office-2", "office-1", "factory-2"),
            SiteSort.TYPE to listOf("factory-1", "factory-2", "home-1", "home-2", "office-2", "office-1"),
            SiteSort.POWER to listOf("home-1", "home-2", "office-2", "office-1", "factory-1", "factory-2"),
            SiteSort.DATE to listOf("home-1", "factory-1", "office-1", "home-2", "office-2", "factory-2"),
            SiteSort.STATUS to listOf("home-1", "factory-1", "office-2", "office-1", "home-2", "factory-2"),
            SiteSort.FORECAST to listOf("home-2", "factory-2", "office-2", "home-1", "office-1", "factory-1")
        )
        showBrowser()
        expected.forEach { (criterion, ascending) ->
            compose.onNodeWithText("Фільтри").performClick()
            choose("Перший критерій", query.primary.label, criterion.label)
            choose("Другий критерій", query.secondary.label, SiteSort.NAME.label)
            compose.onNodeWithTag("filter-list").performScrollToNode(hasTestTag("sort-descending"))
            if (query.descending) compose.onNodeWithTag("sort-descending").performClick()
            closeFilters()
            compose.onNodeWithTag("map-ids").assertTextEquals(ascending.joinToString(","))
            compose.onNodeWithTag("site-list").performScrollToIndex(0)
            compose.onNodeWithTag("site-${ascending.first()}").assertIsDisplayed()
            compose.onNodeWithTag("query-summary").assertTextContains("↑ за зростанням", substring = true)
            compose.onNodeWithText("Фільтри").performClick()
            compose.onNodeWithTag("filter-list").performScrollToNode(hasTestTag("sort-descending"))
            compose.onNodeWithTag("sort-descending").performClick()
            closeFilters()
            compose.onNodeWithTag("map-ids").assertTextEquals(ascending.reversed().joinToString(","))
            compose.onNodeWithTag("site-list").performScrollToIndex(0)
            compose.onNodeWithTag("site-${ascending.last()}").assertIsDisplayed()
            compose.onNodeWithTag("query-summary").assertTextContains("↓ за спаданням", substring = true)
        }
    }

    @Test fun sheetResetIsImmediateAndKeepsTheSheetUsable() {
        query = SiteQuery(search = "немає", type = SiteType.INDUSTRY, status = SiteStatus.MAINTENANCE,
            minPowerKw = "200", maxPowerKw = "20", primary = SiteSort.STATUS, secondary = SiteSort.FORECAST, descending = true)
        showBrowser()
        compose.onNodeWithText("Фільтри").performClick()
        scrollFilter("Скинути")
        compose.onNodeWithText("Скинути").performClick()
        compose.runOnIdle { assertEquals(SiteQuery(), query) }
        compose.onNodeWithTag("filter-list").assertIsDisplayed()
        closeFilters()
        compose.onNodeWithTag("map-ids").assertTextEquals("home-1,home-2,factory-1,office-2,office-1,factory-2")
    }
}
