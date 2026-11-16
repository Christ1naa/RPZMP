package ua.kpi.energyforecast.ui.scenarios

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import ua.kpi.energyforecast.data.*
import ua.kpi.energyforecast.logic.EnergyCalculator
import ua.kpi.energyforecast.model.*

@OptIn(ExperimentalCoroutinesApi::class)
class ScenarioViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val scenario = EnergyScenario("office", "Офіс", ScenarioCategory.OFFICE,
        "Опис", listOf(EnergyDevice("lamp", "Лампа", 100.0, 5.0, 1)))
    private val catalog = MutableStateFlow(ScenarioCatalog(listOf(scenario), 1000L))
    private val calculations = mutableListOf<Int>()
    private var refreshFailure: Exception? = null
    private lateinit var viewModel: ScenarioViewModel

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        val repository = object : ScenarioRepository {
            override fun observeCatalog() = catalog
            override suspend fun refresh() { refreshFailure?.let { throw it } }
            override suspend fun getCachedScenario(id: String) = scenario.takeIf { it.id == id }
            override fun calculateForecast(scenario: EnergyScenario, forecastDays: Int): ForecastResult {
                calculations += forecastDays
                return EnergyCalculator.calculateTotal(scenario.devices, forecastDays)
            }
        }
        viewModel = ScenarioViewModel(repository, ScenarioImporter { _, _ -> }, "owner")
    }

    @After fun tearDown() {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    @Test fun repositoryCalculatesCardsAndDetailsEvenWhenFilteredOut() = runTest(dispatcher) {
        runCurrent()
        assertEquals(listOf(1), calculations)
        viewModel.selectScenario("office")
        viewModel.updateDays("7")
        viewModel.updateQuery("немає")
        assertTrue(viewModel.uiState.value.visibleItems.isEmpty())
        assertEquals(3.5, viewModel.uiState.value.selectedForecast!!.periodConsumptionKwh, 0.000001)
        assertEquals(listOf(1, 30, 7), calculations)
    }

    @Test fun invalidPeriodClearsForecastAndDoesNotCallRepository() = runTest(dispatcher) {
        runCurrent()
        viewModel.selectScenario("office")
        val before = calculations.size
        viewModel.updateDays("366")
        assertNull(viewModel.uiState.value.selectedForecast)
        assertEquals(before, calculations.size)
        viewModel.updateDays("365")
        assertNotNull(viewModel.uiState.value.selectedForecast)
    }

    @Test fun cacheReplacementRecalculatesAndRemovedSelectionClearsForecast() = runTest(dispatcher) {
        runCurrent()
        viewModel.selectScenario("office")
        catalog.value = ScenarioCatalog(listOf(scenario.copy(devices = listOf(
            EnergyDevice("lamp", "Лампа", 200.0, 5.0, 1)))), 2000L)
        runCurrent()
        assertEquals(30.0, viewModel.uiState.value.selectedForecast!!.periodConsumptionKwh, 0.000001)
        catalog.value = ScenarioCatalog(emptyList(), 3000L)
        runCurrent()
        assertNull(viewModel.uiState.value.selectedForecast)
        assertTrue(viewModel.uiState.value.visibleItems.isEmpty())
    }

    @Test fun failedRefreshPreservesCachedForecastAndStopsLoading() = runTest(dispatcher) {
        runCurrent()
        viewModel.selectScenario("office")
        refreshFailure = java.io.IOException("offline")
        viewModel.refresh()
        runCurrent()
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertNotNull(viewModel.uiState.value.refreshError)
        assertEquals(1000L, viewModel.uiState.value.catalog.updatedAtMillis)
        assertNotNull(viewModel.uiState.value.selectedForecast)
    }
}
