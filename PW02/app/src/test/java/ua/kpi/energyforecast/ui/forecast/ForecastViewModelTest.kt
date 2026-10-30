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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import ua.kpi.energyforecast.data.EnergyRepository
import ua.kpi.energyforecast.data.SavedEnergyProfile

@OptIn(ExperimentalCoroutinesApi::class)
class ForecastViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ForecastViewModel
    private lateinit var repository: FakeEnergyRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeEnergyRepository()
        viewModel = ForecastViewModel(
            repository = repository,
            ownerKey = "demo@energy.test",
            calculationDispatcher = dispatcher
        )
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun addingDeviceCalculatesForecastAndEndsLoading() =
        runTest(dispatcher) {
            // Завершуємо початкове завантаження профілю.
            advanceUntilIdle()

            viewModel.saveDevice(device())

            // Очікуємо запис у Repository, оновлення Flow та розрахунок.
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
            advanceUntilIdle()

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

    @Test
    fun newViewModelRestoresSavedProfile() = runTest(dispatcher) {
        viewModel.saveDevice(device())
        viewModel.updateDays("7")
        advanceUntilIdle()

        val restored = ForecastViewModel(
            repository = repository,
            ownerKey = "demo@energy.test",
            calculationDispatcher = dispatcher
        )

        try {
            advanceUntilIdle()

            val state = restored.uiState.value
            assertEquals(1, state.devices.size)
            assertEquals("7", state.daysText)
            assertEquals(
                14.0,
                requireNotNull(state.result).periodConsumptionKwh,
                0.000001
            )
        } finally {
            restored.viewModelScope.cancel()
        }
    }

    @Test
    fun differentOwnerHasIndependentProfile() = runTest(dispatcher) {
        viewModel.saveDevice(device())
        advanceUntilIdle()

        val other = ForecastViewModel(
            repository = repository,
            ownerKey = "other@energy.test",
            calculationDispatcher = dispatcher
        )

        try {
            advanceUntilIdle()

            assertTrue(other.uiState.value.devices.isEmpty())
            assertNull(other.uiState.value.result)
            assertEquals("30", other.uiState.value.daysText)
        } finally {
            other.viewModelScope.cancel()
        }
    }

    @Test
    fun failedWriteShowsErrorWithoutAddingDevice() = runTest(dispatcher) {
        advanceUntilIdle()
        repository.failWrites = true

        viewModel.saveDevice(device())
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.devices.isEmpty())
        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun searchAndSortDoNotChangeTotalForecast() = runTest(dispatcher) {
        viewModel.saveDevice(device())
        advanceUntilIdle()

        val originalResult = viewModel.uiState.value.result

        viewModel.updateSearchQuery("Немає збігів")
        viewModel.updateSort(DeviceSort.CONSUMPTION_DESC)

        val state = viewModel.uiState.value

        assertTrue(state.visibleDevices.isEmpty())
        assertEquals(1, state.devices.size)
        assertEquals(originalResult, state.result)
        assertFalse(state.isLoading)
    }

    private fun device() = EnergyDevice(
        id = "device-1",
        name = "Обігрівач",
        powerWatts = 1000.0,
        hoursPerDay = 2.0,
        deviceCount = 1
    )
}

/** Тестуємо ViewModel без Android і файлової бази даних. */
private class FakeEnergyRepository : EnergyRepository {

    private val profiles =
        mutableMapOf<String, MutableStateFlow<SavedEnergyProfile>>()

    var failWrites: Boolean = false

    private fun profile(ownerKey: String) =
        profiles.getOrPut(ownerKey) {
            MutableStateFlow(SavedEnergyProfile())
        }

    override fun observeProfile(
        ownerKey: String
    ): Flow<SavedEnergyProfile> = profile(ownerKey)

    override suspend fun saveDevice(
        ownerKey: String,
        device: EnergyDevice
    ) {
        check(!failWrites) { "Test write failure" }

        val flow = profile(ownerKey)
        val current = flow.value
        val exists = current.devices.any { it.id == device.id }

        require(exists || current.devices.size < 50)

        val devices = if (exists) {
            current.devices.map {
                if (it.id == device.id) device else it
            }
        } else {
            current.devices + device
        }

        flow.value = current.copy(devices = devices)
    }

    override suspend fun deleteDevice(
        ownerKey: String,
        deviceId: String
    ) {
        check(!failWrites) { "Test write failure" }

        val flow = profile(ownerKey)
        flow.value = flow.value.copy(
            devices = flow.value.devices.filterNot { it.id == deviceId }
        )
    }

    override suspend fun saveForecastDays(
        ownerKey: String,
        days: Int
    ) {
        check(!failWrites) { "Test write failure" }
        require(days in 1..365)

        val flow = profile(ownerKey)
        flow.value = flow.value.copy(forecastDays = days)
    }
}