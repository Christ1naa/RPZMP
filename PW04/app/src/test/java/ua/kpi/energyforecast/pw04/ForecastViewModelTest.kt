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
                        while (true) {
                            emit(MeterFrame(DemoSites.sites.associate { it.id to 1000.0 }, Instant.EPOCH))
                            delay(1000)
                        }
                    } finally { stops++ }
                }
                override fun demandFactor() = flow { while (true) { emit(1.0); delay(1500) } }
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
            var forecastFailures = 0
            val source = object : TelemetrySource {
                override fun meter() = flow {
                    starts++
                    try { emit(MeterFrame(DemoSites.sites.associate { it.id to 1000.0 }, Instant.EPOCH)); awaitCancellation() }
                    finally { stops++ }
                }
                override fun demandFactor() = flow { while (true) { emit(1.0); delay(1500) } }
            }
            val model = ForecastViewModel(SavedStateHandle(), engine = TelemetryEngine(DemoSites.sites, source),
                failureAction = { failures++ }, forecastFailureAction = { forecastFailures++ })
            store.put("forecast", model)
            backgroundScope.launch { model.uiState.collect() }
            runCurrent()
            model.simulateFailure()
            model.simulateForecastFailure()
            model.restart()
            runCurrent()
            assertEquals(1, failures)
            assertEquals(1, forecastFailures)
            assertEquals(2, starts)
            assertEquals(1, stops)
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }

    @Test fun restartFromFailedStartsANewHealthySession() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            var starts = 0
            val source = object : TelemetrySource {
                override fun meter() = flow {
                    starts++
                    if (starts == 1) throw IllegalStateException()
                    emit(MeterFrame(DemoSites.sites.associate { it.id to 1000.0 }, Instant.EPOCH))
                    awaitCancellation()
                }
                override fun demandFactor() = flow { emit(1.0); awaitCancellation() }
            }
            val model = ForecastViewModel(SavedStateHandle(), engine = TelemetryEngine(DemoSites.sites, source))
            store.put("forecast", model)
            backgroundScope.launch { model.uiState.collect() }
            runCurrent()
            assertEquals(StreamPhase.FAILED, model.uiState.value.telemetry.phase)
            model.restart()
            runCurrent()
            assertEquals(StreamPhase.LIVE, model.uiState.value.telemetry.phase)
            assertEquals(0, model.uiState.value.telemetry.retries)
            assertEquals(6, model.uiState.value.visible.size)
            assertEquals(2, starts)
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }

    @Test fun resubscriptionAfterStopHasNoOldLiveSnapshotAndRestartsBothSources() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            var meterStarts = 0
            var meterStops = 0
            var modelStarts = 0
            var modelStops = 0
            val source = object : TelemetrySource {
                override fun meter() = flow {
                    meterStarts++
                    try {
                        delay(100)
                        while (true) {
                            emit(MeterFrame(DemoSites.sites.associate { it.id to meterStarts * 1000.0 }, Instant.EPOCH))
                            delay(1000)
                        }
                    } finally { meterStops++ }
                }
                override fun demandFactor() = flow {
                    modelStarts++
                    try { while (true) { emit(1.0); delay(1500) } }
                    finally { modelStops++ }
                }
            }
            val model = ForecastViewModel(SavedStateHandle(), engine = TelemetryEngine(DemoSites.sites, source))
            store.put("forecast", model)
            val first = backgroundScope.launch { model.uiState.collect() }
            advanceTimeBy(101); runCurrent()
            assertEquals(StreamPhase.LIVE, model.uiState.value.telemetry.phase)
            first.cancelAndJoin(); runCurrent()
            advanceTimeBy(5001); runCurrent()
            assertEquals(1, meterStops)
            assertEquals(1, modelStops)
            val states = mutableListOf<StreamPhase>()
            backgroundScope.launch { model.uiState.collect { states += it.telemetry.phase } }
            runCurrent()
            assertTrue(states.isNotEmpty())
            assertFalse(states.contains(StreamPhase.LIVE))
            assertEquals(2, meterStarts)
            assertEquals(2, modelStarts)
            advanceTimeBy(101); runCurrent()
            assertEquals(StreamPhase.LIVE, model.uiState.value.telemetry.phase)
            assertEquals(2.0, model.uiState.value.telemetry.readings.first().measuredPowerKw, 0.0)
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }

    @Test fun copiedSavedStateRestoresSelectedIdAndKeepsItDuringReadingUpdates() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val frames = MutableStateFlow(MeterFrame(DemoSites.sites.associate { it.id to 1000.0 }, Instant.EPOCH))
            val source = object : TelemetrySource {
                override fun meter() = frames
                override fun demandFactor() = flow { emit(1.0); awaitCancellation() }
            }
            val saved = SavedStateHandle()
            val first = ForecastViewModel(saved, engine = TelemetryEngine(DemoSites.sites, source))
            store.put("first", first)
            backgroundScope.launch { first.uiState.collect() }
            runCurrent()
            first.updateQuery(SiteQuery(search = "Офіс", type = SiteType.OFFICE))
            runCurrent()
            first.select("office-1")
            runCurrent()
            // Новий handle з копією значень, а не повторне використання того самого об'єкта.
            val restoredState = SavedStateHandle(saved.keys().associateWith { saved.get<Any?>(it) })
            val restored = ForecastViewModel(restoredState, engine = TelemetryEngine(DemoSites.sites, source))
            store.put("restored", restored)
            backgroundScope.launch { restored.uiState.collect() }
            runCurrent()
            assertEquals("Офіс", restored.uiState.value.query.search)
            assertEquals("office-1", restored.uiState.value.selectedId)
            assertEquals(2, restored.uiState.value.visible.size)
            frames.value = MeterFrame(DemoSites.sites.associate { it.id to 2000.0 }, Instant.EPOCH.plusSeconds(1))
            runCurrent()
            assertEquals("office-1", restored.uiState.value.selectedId)
            assertEquals(2.0, restored.uiState.value.visible.first().measuredPowerKw, 0.0)
            assertEquals(Instant.EPOCH.plusSeconds(1), restored.uiState.value.visible.first().sampledAt)
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }

    @Test fun everyQueryFieldIsRestoredAndChangingOrderPreservesVisibleSelection() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val source = object : TelemetrySource {
                override fun meter() = flow { emit(MeterFrame(DemoSites.sites.associate { it.id to 1000.0 }, Instant.EPOCH)); awaitCancellation() }
                override fun demandFactor() = flow { emit(1.0); awaitCancellation() }
            }
            val saved = SavedStateHandle()
            val model = ForecastViewModel(saved, engine = TelemetryEngine(DemoSites.sites, source))
            store.put("model", model)
            backgroundScope.launch { model.uiState.collect() }
            runCurrent()
            model.select("office-1"); runCurrent()
            val base = SiteQuery(search = " ОФІС ", type = SiteType.OFFICE,
                status = ua.kpi.energyforecast.pw04.model.SiteStatus.ACTIVE,
                minPowerKw = "20,0", maxPowerKw = "50.0")
            for (sort in SiteSort.entries) for (descending in listOf(false, true)) {
                model.updateQuery(base.copy(primary = sort, secondary = SiteSort.DATE, descending = descending))
                runCurrent()
                assertEquals("office-1", model.uiState.value.selectedId)
                assertEquals(setOf("office-1", "office-2"), model.uiState.value.visible.map { it.site.id }.toSet())
            }
            val expected = model.uiState.value.query
            val restored = ForecastViewModel(SavedStateHandle(saved.keys().associateWith { saved.get<Any?>(it) }),
                engine = TelemetryEngine(DemoSites.sites, source))
            store.put("restored", restored)
            backgroundScope.launch { restored.uiState.collect() }
            runCurrent()
            assertEquals(expected, restored.uiState.value.query)
            assertEquals("office-1", restored.uiState.value.selectedId)
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }

    @Test fun hiddenSelectionIsClearedAndDoesNotReappearAfterReset() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val source = object : TelemetrySource {
                override fun meter() = flow { emit(MeterFrame(DemoSites.sites.associate { it.id to 1000.0 }, Instant.EPOCH)); awaitCancellation() }
                override fun demandFactor() = flow { emit(1.0); awaitCancellation() }
            }
            val saved = SavedStateHandle()
            val model = ForecastViewModel(saved, engine = TelemetryEngine(DemoSites.sites, source))
            store.put("model", model)
            backgroundScope.launch { model.uiState.collect() }
            runCurrent()
            model.select("office-1"); runCurrent()
            model.updateQuery(SiteQuery(minPowerKw = "100")); runCurrent()
            assertNull(model.uiState.value.selectedId)
            assertNull(saved.get<String>("selected"))
            model.updateQuery(SiteQuery()); runCurrent()
            assertNull(model.uiState.value.selectedId)
            model.select("home-1"); runCurrent()
            model.updateQuery(SiteQuery(maxPowerKw = "0x1.0p3")); runCurrent()
            assertNotNull(model.uiState.value.query.error)
            assertTrue(model.uiState.value.visible.isEmpty())
            assertNull(model.uiState.value.selectedId)
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }

    @Test fun newForecastCanReorderTheListWithoutChangingSelectedId() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val frames = MutableStateFlow(MeterFrame(DemoSites.sites.associate { it.id to 1000.0 }, Instant.EPOCH))
            val source = object : TelemetrySource {
                override fun meter() = frames
                override fun demandFactor() = flow { emit(1.0); awaitCancellation() }
            }
            val model = ForecastViewModel(SavedStateHandle(), engine = TelemetryEngine(DemoSites.sites, source))
            store.put("model", model)
            backgroundScope.launch { model.uiState.collect() }
            runCurrent()
            model.updateQuery(SiteQuery(primary = SiteSort.FORECAST)); runCurrent()
            model.select("home-1"); runCurrent()
            assertEquals(2, model.uiState.value.visible.indexOfFirst { it.site.id == "home-1" })
            frames.value = MeterFrame(DemoSites.sites.associate { it.id to if (it.id == "home-1") 10000.0 else 1000.0 }, Instant.EPOCH.plusSeconds(1))
            runCurrent()
            assertEquals("home-1", model.uiState.value.visible.last().site.id)
            assertEquals("home-1", model.uiState.value.selectedId)
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }

    @Test fun repeatedSelectionIsANewActionButReadingsAndSortingAreNot() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val store = ViewModelStore()
        try {
            val frames = MutableStateFlow(MeterFrame(DemoSites.sites.associate { it.id to 1000.0 }, Instant.EPOCH))
            val source = object : TelemetrySource {
                override fun meter() = frames
                override fun demandFactor() = flow { emit(1.0); awaitCancellation() }
            }
            val model = ForecastViewModel(SavedStateHandle(), engine = TelemetryEngine(DemoSites.sites, source))
            store.put("model", model)
            backgroundScope.launch { model.uiState.collect() }
            runCurrent()
            model.select("home-1"); runCurrent()
            val first = model.uiState.value.selectionRevision
            model.select("home-1"); runCurrent()
            assertEquals("home-1", model.uiState.value.selectedId)
            assertEquals(first + 1, model.uiState.value.selectionRevision)
            frames.value = MeterFrame(DemoSites.sites.associate { it.id to 2000.0 }, Instant.EPOCH.plusSeconds(1))
            model.updateQuery(SiteQuery(primary = SiteSort.POWER, descending = true)); runCurrent()
            assertEquals(first + 1, model.uiState.value.selectionRevision)
            model.select("not-visible"); runCurrent()
            assertEquals(first + 1, model.uiState.value.selectionRevision)
            model.updateQuery(SiteQuery(type = SiteType.OFFICE)); runCurrent()
            assertNull(model.uiState.value.selectedId)
            model.select("home-1"); runCurrent()
            assertEquals(first + 1, model.uiState.value.selectionRevision)
            model.select("office-1"); runCurrent()
            assertEquals(first + 2, model.uiState.value.selectionRevision)
        } finally { store.clear(); runCurrent(); Dispatchers.resetMain() }
    }
}
