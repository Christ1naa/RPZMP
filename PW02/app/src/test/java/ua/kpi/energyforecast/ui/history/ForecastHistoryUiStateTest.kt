package ua.kpi.energyforecast.ui.history

import org.junit.Assert.*
import org.junit.Test
import ua.kpi.energyforecast.model.EnergyDevice
import ua.kpi.energyforecast.model.ForecastResult
import ua.kpi.energyforecast.model.SavedForecast

class ForecastHistoryUiStateTest {

    @Test
    fun searchFindsTitleIgnoringCaseAndOuterSpaces() {
        val state = ForecastHistoryUiState(
            forecasts = forecasts(),
            searchQuery = "  КВАРТИРА  "
        )

        assertEquals(
            listOf("home"),
            state.visibleForecasts.map { it.id }
        )
    }

    @Test
    fun searchFindsArchivedDeviceName() {
        val state = ForecastHistoryUiState(
            forecasts = forecasts(),
            searchQuery = "ЧАЙНИК"
        )

        assertEquals(
            listOf("home"),
            state.visibleForecasts.map { it.id }
        )
    }

    @Test
    fun weekFilterIncludesSevenDaysAndExcludesEight() {
        val records = listOf(
            forecast(id = "seven", days = 7),
            forecast(id = "eight", days = 8)
        )

        val state = ForecastHistoryUiState(
            forecasts = records,
            periodFilter = HistoryPeriodFilter.UP_TO_WEEK
        )

        assertEquals(
            listOf("seven"),
            state.visibleForecasts.map { it.id }
        )

        assertEquals(
            listOf("eight"),
            state.copy(
                periodFilter = HistoryPeriodFilter.OVER_WEEK
            ).visibleForecasts.map { it.id }
        )
    }

    @Test
    fun searchAndPeriodFilterApplyTogether() {
        val state = ForecastHistoryUiState(
            forecasts = listOf(
                forecast(
                    id = "short",
                    title = "Квартира",
                    days = 7
                ),
                forecast(
                    id = "long",
                    title = "Квартира",
                    days = 10
                ),
                forecast(
                    id = "other",
                    title = "Офіс",
                    days = 7
                )
            ),
            searchQuery = "квартира",
            periodFilter = HistoryPeriodFilter.UP_TO_WEEK
        )

        assertEquals(
            listOf("short"),
            state.visibleForecasts.map { it.id }
        )

        // Фільтрація не видаляє записи з повного списку.
        assertEquals(3, state.forecasts.size)
    }

    @Test
    fun newestSortUsesCreationTime() {
        val state = ForecastHistoryUiState(
            forecasts = forecasts(),
            sort = HistorySort.NEWEST
        )

        assertEquals(
            listOf("office", "home"),
            state.visibleForecasts.map { it.id }
        )
    }

    @Test
    fun consumptionSortUsesPeriodTotalInsteadOfDailyValue() {
        val state = ForecastHistoryUiState(
            forecasts = listOf(
                forecast(
                    id = "high_daily",
                    days = 1,
                    daily = 5.0
                ),
                forecast(
                    id = "high_period",
                    days = 10,
                    daily = 2.0
                )
            ),
            sort = HistorySort.CONSUMPTION_DESC
        )

        assertEquals(
            listOf("high_period", "high_daily"),
            state.visibleForecasts.map { it.id }
        )
    }

    @Test
    fun equalConsumptionUsesNewestRecordThenId() {
        val state = ForecastHistoryUiState(
            forecasts = listOf(
                forecast(id = "old", createdAt = 100L),
                forecast(id = "b", createdAt = 200L),
                forecast(id = "a", createdAt = 200L)
            ),
            sort = HistorySort.CONSUMPTION_DESC
        )

        assertEquals(
            listOf("a", "b", "old"),
            state.visibleForecasts.map { it.id }
        )
    }

    @Test
    fun unmatchedSearchReturnsEmptyVisibleList() {
        val records = forecasts()

        val state = ForecastHistoryUiState(
            forecasts = records,
            searchQuery = "неіснуючий прилад"
        )

        assertTrue(state.visibleForecasts.isEmpty())
        assertEquals(records, state.forecasts)
    }

    private fun forecasts() = listOf(
        forecast(
            id = "home",
            title = "Квартира",
            deviceName = "Електрочайник",
            days = 10,
            daily = 2.76,
            createdAt = 100L
        ),
        forecast(
            id = "office",
            title = "Офіс",
            deviceName = "Комп’ютер",
            days = 7,
            daily = 3.0,
            createdAt = 200L
        )
    )

    private fun forecast(
        id: String,
        title: String = "Прогноз",
        deviceName: String = "Прилад",
        days: Int = 7,
        daily: Double = 1.0,
        createdAt: Long = 100L
    ) = SavedForecast(
        id = id,
        title = title,
        createdAt = createdAt,
        result = ForecastResult(
            dailyConsumptionKwh = daily,
            periodConsumptionKwh = daily * days,
            forecastDays = days
        ),
        devices = listOf(
            EnergyDevice(
                id = "device_$id",
                name = deviceName,
                powerWatts = daily * 1000.0,
                hoursPerDay = 1.0,
                deviceCount = 1
            )
        )
    )
}