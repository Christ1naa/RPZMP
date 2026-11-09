package ua.kpi.energyforecast.ui.scenarios

import org.junit.Assert.*
import org.junit.Test
import ua.kpi.energyforecast.data.ScenarioCatalog
import ua.kpi.energyforecast.model.EnergyDevice
import ua.kpi.energyforecast.model.EnergyScenario
import ua.kpi.energyforecast.model.ScenarioCategory

class ScenarioSelectionTest {

    private fun scenario(
        id: String,
        name: String,
        category: ScenarioCategory,
        powerWatts: Double
    ): EnergyScenario {
        return EnergyScenario(
            id = id,
            name = name,
            category = category,
            description = "Тестовий сценарій",
            devices = listOf(
                EnergyDevice(
                    id = "device",
                    name = "Прилад",
                    powerWatts = powerWatts,
                    hoursPerDay = 1.0,
                    deviceCount = 1
                )
            )
        )
    }

    private fun catalog(): List<EnergyScenario> {
        return listOf(
            scenario(
                "office",
                "Домашній офіс",
                ScenarioCategory.OFFICE,
                500.0
            ),
            scenario(
                "home",
                "Вечір удома",
                ScenarioCategory.HOME,
                1000.0
            ),
            scenario(
                "study",
                "Навчальний день",
                ScenarioCategory.MIXED,
                100.0
            )
        )
    }

    @Test
    fun searchIgnoresCaseAndSurroundingSpaces() {
        val result = selectScenarios(
            scenarios = catalog(),
            query = "  ОФІС  ",
            category = null,
            sort = ScenarioSort.NAME_ASC
        )

        assertEquals(
            listOf("office"),
            result.map { it.scenario.id }
        )
    }

    @Test
    fun categoryAndSearchAreAppliedTogether() {
        val result = selectScenarios(
            scenarios = catalog(),
            query = "д",
            category = ScenarioCategory.HOME,
            sort = ScenarioSort.NAME_ASC
        )

        assertEquals(
            listOf("home"),
            result.map { it.scenario.id }
        )
    }

    @Test
    fun sortsByNumericDailyConsumption() {
        val ascending = selectScenarios(
            catalog(),
            "",
            null,
            ScenarioSort.DAILY_ASC
        )

        val descending = selectScenarios(
            catalog(),
            "",
            null,
            ScenarioSort.DAILY_DESC
        )

        assertEquals(
            listOf("study", "office", "home"),
            ascending.map { it.scenario.id }
        )

        assertEquals(
            listOf("home", "office", "study"),
            descending.map { it.scenario.id }
        )

        assertEquals(
            0.1,
            ascending.first().dailyConsumptionKwh,
            0.000001
        )
    }

    @Test
    fun filteringDoesNotChangeOriginalCatalog() {
        val original = catalog()
        val before = original.toList()

        val result = selectScenarios(
            original,
            "немає такої назви",
            null,
            ScenarioSort.NAME_ASC
        )

        assertTrue(result.isEmpty())
        assertEquals(before, original)
        assertEquals(3, original.size)
    }

    @Test
    fun detailsRemainAvailableWhenFilteredOutOfList() {
        val state = ScenarioUiState(
            catalog = ScenarioCatalog(
                scenarios = catalog(),
                updatedAtMillis = 1000L
            ),
            isLoadingCache = false,
            query = "нічого",
            selectedScenarioId = "office",
            daysText = "30"
        )

        assertTrue(state.visibleItems.isEmpty())
        assertEquals("office", state.selectedScenario?.id)

        val forecast = requireNotNull(state.selectedForecast)

        assertEquals(
            0.5,
            forecast.dailyConsumptionKwh,
            0.000001
        )
        assertEquals(
            15.0,
            forecast.periodConsumptionKwh,
            0.000001
        )
    }

    @Test
    fun invalidPeriodDoesNotProduceForecast() {
        val state = ScenarioUiState(
            catalog = ScenarioCatalog(
                scenarios = catalog(),
                updatedAtMillis = 1000L
            ),
            selectedScenarioId = "office",
            daysText = "366"
        )

        assertNull(state.forecastDays)
        assertNull(state.selectedForecast)

        assertEquals(
            365,
            state.copy(daysText = "365").forecastDays
        )
        assertNotNull(
            state.copy(daysText = "365").selectedForecast
        )
    }
}