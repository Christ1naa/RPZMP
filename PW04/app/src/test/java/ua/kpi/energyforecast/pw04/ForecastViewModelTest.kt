package ua.kpi.energyforecast.pw04

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import ua.kpi.energyforecast.pw04.data.*
import ua.kpi.energyforecast.pw04.logic.*
import ua.kpi.energyforecast.pw04.model.SiteType
import ua.kpi.energyforecast.pw04.ui.ForecastViewModel
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ForecastViewModelTest {
    @Test fun selectionFiltersRestoreAndSharingUseOneGenerator() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            var starts = 0
            var stops = 0
            val source = object : TelemetrySource {
                override fun meter() = flow {
                    starts++
                    try {
                        emit(MeterFrame(DemoSites.sites.associate { it.id to 1000.0 }, Instant.EPOCH))
                        awaitCancellation()
                    } finally { stops++ }
                }
                override fun demandFactor() = flowOf(1.0)
            }
            val saved = SavedStateHandle()
            val model = ForecastViewModel(saved, engine = TelemetryEngine(DemoSites.sites, source))
            store.put("forecast", model)
            val first = backgroundScope.launch { model.uiState.collect() }
            val second = backgroundScope.launch { model.telemetry.collect() }
            runCurrent()
            assertEquals(1, starts)
            assertEquals(6, model.uiState.value.visible.size)
            model.select("home-1")
            runCurrent()
            assertEquals("home-1", model.uiState.value.selectedId)
            model.updateQuery(SiteQuery(type = SiteType.OFFICE, primary = SiteSort.POWER, descending = true))
            runCurrent()
            assertNull(model.uiState.value.selectedId)
            assertEquals(listOf("office-1", "office-2"), model.uiState.value.visible.map { it.site.id })
            assertEquals(SiteType.OFFICE.name, saved.get<String>("type"))
            val restored = ForecastViewModel(saved, engine = TelemetryEngine(DemoSites.sites, source))
            store.put("restored", restored)
            assertEquals(SiteType.OFFICE, restored.uiState.value.query.type)
            assertTrue(restored.uiState.value.query.descending)
            first.cancel(); second.cancel(); runCurrent()
            advanceTimeBy(5001); runCurrent()
            assertEquals(1, stops)
        } finally {
            store.clear()
            runCurrent()
            Dispatchers.resetMain()
        }
    }

    @Test fun restartCancelsOldCollectionAndFailureActionIsWired() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            var starts = 0
            var stops = 0
            var failures = 0
            val source = object : TelemetrySource {
                override fun meter() = flow {
                    starts++
                    try { emit(MeterFrame(DemoSites.sites.associate { it.id to 1000.0 }, Instant.EPOCH)); awaitCancellation() }
                    finally { stops++ }
                }
                override fun demandFactor() = flowOf(1.0)
            }
            val model = ForecastViewModel(SavedStateHandle(), engine = TelemetryEngine(DemoSites.sites, source), failureAction = { failures++ })
            store.put("forecast", model)
            backgroundScope.launch { model.uiState.collect() }
            runCurrent()
            model.simulateFailure()
            model.restart()
            runCurrent()
            assertEquals(1, failures)
            assertEquals(2, starts)
            assertEquals(1, stops)
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }
}
