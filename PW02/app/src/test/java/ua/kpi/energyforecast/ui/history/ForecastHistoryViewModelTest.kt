package ua.kpi.energyforecast.ui.history

import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import ua.kpi.energyforecast.data.ForecastHistoryRepository
import ua.kpi.energyforecast.logic.EnergyCalculator
import ua.kpi.energyforecast.model.EnergyDevice
import ua.kpi.energyforecast.model.SavedForecast

@OptIn(ExperimentalCoroutinesApi::class)
class ForecastHistoryViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var store: ViewModelStore

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        store = ViewModelStore()
    }

    @After
    fun tearDown() {
        // Скасовує підписки та операції ViewModel після кожного тесту.
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun initialLoadReceivesHistoryAndEndsLoading() = runTest(dispatcher) {
        val repository = TestHistoryRepository()
        repository.records.value = listOf(savedForecast())

        val viewModel = createViewModel(repository)

        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.loadError)
        assertEquals(
            listOf("saved"),
            viewModel.uiState.value.forecasts.map { it.id }
        )
        assertEquals(OWNER, repository.observedOwner)
    }

    @Test
    fun repeatedSaveIsBlockedAndOriginalParametersAreCopied() =
        runTest(dispatcher) {
            val repository = TestHistoryRepository()
            val gate = CompletableDeferred<Unit>()
            repository.saveGate = gate

            val viewModel = createViewModel(repository)
            advanceUntilIdle()

            val devices = mutableListOf(device())

            viewModel.saveForecast("Перший прогноз", devices, 10)
            viewModel.saveForecast("Повторне натискання", devices, 10)

            // Зміна початкового списку не повинна змінювати вже надіслану копію.
            devices.clear()

            assertTrue(viewModel.uiState.value.isSaving)

            runCurrent()

            assertEquals(1, repository.saveCalls)
            assertEquals(listOf(device()), repository.lastSavedDevices)
            assertEquals(OWNER, repository.savedOwner)
            assertTrue(viewModel.uiState.value.isSaving)

            gate.complete(Unit)
            advanceUntilIdle()

            val state = viewModel.uiState.value

            assertFalse(state.isSaving)
            assertNull(state.operationError)
            assertNotNull(state.message)
            assertEquals(1, state.forecasts.size)
            assertEquals("Перший прогноз", state.forecasts.single().title)
            assertEquals(
                20.0,
                state.forecasts.single().result.periodConsumptionKwh,
                0.000001
            )
        }

    @Test
    fun failedSaveShowsErrorAndAllowsRetry() = runTest(dispatcher) {
        val repository = TestHistoryRepository()
        repository.failSaves = true

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.saveForecast("Прогноз", listOf(device()), 10)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSaving)
        assertNotNull(viewModel.uiState.value.operationError)
        assertTrue(viewModel.uiState.value.forecasts.isEmpty())
        assertNull(viewModel.uiState.value.message)

        repository.failSaves = false

        viewModel.saveForecast("Повторна спроба", listOf(device()), 10)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSaving)
        assertNull(viewModel.uiState.value.operationError)
        assertNotNull(viewModel.uiState.value.message)
        assertEquals(1, viewModel.uiState.value.forecasts.size)
    }

    @Test
    fun failedDeletePreservesRecordAndAllowsRetry() = runTest(dispatcher) {
        val repository = TestHistoryRepository()
        repository.records.value = listOf(savedForecast())
        repository.failDeletes = true

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.deleteForecast("saved")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isDeleting)
        assertNotNull(viewModel.uiState.value.operationError)
        assertEquals(1, viewModel.uiState.value.forecasts.size)

        repository.failDeletes = false

        viewModel.deleteForecast("saved")
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isDeleting)
        assertNull(viewModel.uiState.value.operationError)
        assertTrue(viewModel.uiState.value.forecasts.isEmpty())
        assertNotNull(viewModel.uiState.value.message)
    }

    @Test
    fun retryAfterReadFailureRestoresObservation() = runTest(dispatcher) {
        val repository = TestHistoryRepository()
        repository.failReads = true

        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.loadError)

        repository.failReads = false
        repository.records.value = listOf(savedForecast())

        viewModel.retryLoad()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.loadError)
        assertEquals(1, viewModel.uiState.value.forecasts.size)

        // Після retry підписка має отримувати і наступні зміни.
        repository.records.value = emptyList()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.forecasts.isEmpty())
    }

    private fun createViewModel(
        repository: ForecastHistoryRepository
    ): ForecastHistoryViewModel {
        return ForecastHistoryViewModel(repository, OWNER).also {
            store.put("history", it)
        }
    }

    private fun device() = EnergyDevice(
        id = "device",
        name = "Обігрівач",
        powerWatts = 1000.0,
        hoursPerDay = 2.0,
        deviceCount = 1
    )

    private fun savedForecast() = SavedForecast(
        id = "saved",
        title = "Збережений прогноз",
        createdAt = 100L,
        result = EnergyCalculator.calculateTotal(
            devices = listOf(device()),
            forecastDays = 10
        ),
        devices = listOf(device())
    )

    companion object {
        private const val OWNER = "demo@energy.test"
    }
}

/**
 * Керована підміна сховища для перевірки поведінки ViewModel.
 * Реальну Room Database перевіряють окремі інструментальні тести.
 */
private class TestHistoryRepository : ForecastHistoryRepository {

    val records = MutableStateFlow<List<SavedForecast>>(emptyList())

    var failReads = false
    var failSaves = false
    var failDeletes = false

    var saveGate: CompletableDeferred<Unit>? = null
    var saveCalls = 0

    var observedOwner: String? = null
    var savedOwner: String? = null
    var lastSavedDevices: List<EnergyDevice> = emptyList()

    override fun observeHistory(
        ownerKey: String
    ): Flow<List<SavedForecast>> = flow {
        observedOwner = ownerKey
        check(!failReads) { "Test read failure" }
        emitAll(records)
    }

    override fun observeForecast(
        ownerKey: String,
        forecastId: String
    ): Flow<SavedForecast?> = observeHistory(ownerKey).map { forecasts ->
        forecasts.firstOrNull { it.id == forecastId }
    }

    override suspend fun saveForecast(
        ownerKey: String,
        title: String,
        devices: List<EnergyDevice>,
        forecastDays: Int
    ): String {
        saveCalls += 1
        savedOwner = ownerKey
        lastSavedDevices = devices.toList()

        saveGate?.await()
        check(!failSaves) { "Test save failure" }

        val id = "forecast_$saveCalls"

        records.value = records.value + SavedForecast(
            id = id,
            title = title,
            createdAt = saveCalls.toLong(),
            result = EnergyCalculator.calculateTotal(
                devices = devices,
                forecastDays = forecastDays
            ),
            devices = devices.toList()
        )

        return id
    }

    override suspend fun deleteForecast(
        ownerKey: String,
        forecastId: String
    ) {
        check(!failDeletes) { "Test delete failure" }

        records.value = records.value.filterNot {
            it.id == forecastId
        }
    }
}