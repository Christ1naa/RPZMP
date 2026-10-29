package ua.kpi.energyforecast.ui.forecast

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import ua.kpi.energyforecast.model.EnergyDevice

@OptIn(ExperimentalCoroutinesApi::class)
class ForecastViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ForecastViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = ForecastViewModel(dispatcher)
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun addingDeviceCalculatesForecastAndEndsLoading() =
        runTest(dispatcher) {
            viewModel.saveDevice(device())

            assertTrue(viewModel.uiState.value.isLoading)
            assertNull(viewModel.uiState.value.result)

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNull(state.error)
            assertEquals(1, state.devices.size)
            assertEquals(
                60.0,
                requireNotNull(state.result).periodConsumptionKwh,
                0.000001
            )
        }

    @Test
    fun editingDeviceReplacesPositionAndRecalculates() =
        runTest(dispatcher) {
            viewModel.saveDevice(device())
            advanceUntilIdle()

            viewModel.saveDevice(device().copy(deviceCount = 2))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(1, state.devices.size)
            assertEquals(2, state.totalDeviceCount)
            assertEquals(
                120.0,
                requireNotNull(state.result).periodConsumptionKwh,
                0.000001
            )
        }

    @Test
    fun invalidPeriodHidesResultAndCanBeCorrected() =
        runTest(dispatcher) {
            viewModel.saveDevice(device())
            advanceUntilIdle()

            viewModel.updateDays("0")

            assertNull(viewModel.uiState.value.result)
            assertNull(viewModel.uiState.value.forecastDays)
            assertFalse(viewModel.uiState.value.isLoading)

            viewModel.updateDays("7")
            advanceUntilIdle()

            assertEquals(
                14.0,
                requireNotNull(viewModel.uiState.value.result)
                    .periodConsumptionKwh,
                0.000001
            )
        }

    @Test
    fun latestPeriodWinsWhenParametersChangeRapidly() =
        runTest(dispatcher) {
            viewModel.saveDevice(device())
            viewModel.updateDays("7")
            viewModel.updateDays("10")

            advanceUntilIdle()

            val result = requireNotNull(viewModel.uiState.value.result)
            assertEquals(10, result.forecastDays)
            assertEquals(20.0, result.periodConsumptionKwh, 0.000001)
        }

    @Test
    fun deletingLastDeviceClearsForecast() =
        runTest(dispatcher) {
            viewModel.saveDevice(device())
            advanceUntilIdle()

            viewModel.deleteDevice("device-1")

            assertTrue(viewModel.uiState.value.devices.isEmpty())
            assertNull(viewModel.uiState.value.result)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    @Test
    fun invalidDeviceShowsErrorWithoutChangingList() =
        runTest(dispatcher) {
            viewModel.saveDevice(device().copy(powerWatts = -1.0))

            assertTrue(viewModel.uiState.value.devices.isEmpty())
            assertNotNull(viewModel.uiState.value.error)
            assertFalse(viewModel.uiState.value.isLoading)
        }

    private fun device() = EnergyDevice(
        id = "device-1",
        name = "Обігрівач",
        powerWatts = 1000.0,
        hoursPerDay = 2.0,
        deviceCount = 1
    )
}