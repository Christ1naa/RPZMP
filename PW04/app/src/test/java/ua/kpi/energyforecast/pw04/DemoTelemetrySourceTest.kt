package ua.kpi.energyforecast.pw04

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import ua.kpi.energyforecast.pw04.data.*
import ua.kpi.energyforecast.pw04.model.SiteStatus

@OptIn(ExperimentalCoroutinesApi::class)
class DemoTelemetrySourceTest {
    @Test fun realDemoColdMeterProducesChangingValidWattsAndZeroDuringMaintenance() = runTest {
        val source = DemoTelemetrySource(DemoSites.sites)
        val frames = source.meter().take(2).toList()
        assertEquals(2000L, testScheduler.currentTime)
        assertNotEquals(frames[0].watts["home-1"], frames[1].watts["home-1"])
        DemoSites.sites.forEach { site ->
            assertTrue(frames.all { it.watts.getValue(site.id).isFinite() && it.watts.getValue(site.id) >= 0 })
            if (site.status == SiteStatus.MAINTENANCE) assertTrue(frames.all { it.watts[site.id] == 0.0 })
        }
    }

    @Test fun realDemoMeterFailureIsVisibleThenAutomaticallyRecovers() = runTest {
        val source = DemoTelemetrySource(DemoSites.sites)
        val states = mutableListOf<TelemetryState>()
        val job = backgroundScope.launch { TelemetryEngine(DemoSites.sites, source).observe().collect { states += it } }
        advanceTimeBy(1001); runCurrent()
        assertEquals(StreamPhase.LIVE, states.last().phase)
        source.simulateFailure()
        advanceTimeBy(1000); runCurrent()
        assertEquals(StreamPhase.RETRYING, states.last().meterStatus.phase)
        assertTrue(states.last().isStale)
        val frozen = states.last().readings
        advanceTimeBy(1500); runCurrent()
        assertEquals(frozen, states.last().readings)
        advanceTimeBy(1000); runCurrent()
        assertEquals(StreamPhase.LIVE, states.last().phase)
        assertEquals(1, states.last().meterStatus.retries)
        assertFalse(states.last().isStale)
        job.cancelAndJoin()
    }

    @Test fun realDemoForecastFailureIsVisibleThenAutomaticallyRecovers() = runTest {
        val source = DemoTelemetrySource(DemoSites.sites)
        val states = mutableListOf<TelemetryState>()
        val job = backgroundScope.launch { TelemetryEngine(DemoSites.sites, source).observe().collect { states += it } }
        advanceTimeBy(1001); runCurrent()
        source.simulateForecastFailure()
        advanceTimeBy(500); runCurrent()
        assertEquals(StreamPhase.RETRYING, states.last().forecastStatus.phase)
        assertEquals(StreamPhase.LIVE, states.last().meterStatus.phase)
        assertTrue(states.last().isStale)
        advanceTimeBy(1000); runCurrent()
        assertEquals(StreamPhase.LIVE, states.last().phase)
        assertEquals(1, states.last().forecastStatus.retries)
        job.cancelAndJoin()
    }
}
