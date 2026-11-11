package ua.kpi.energyforecast.pw04

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import ua.kpi.energyforecast.pw04.data.*
import java.io.IOException
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class TelemetryEngineTest {
    private val sites = DemoSites.sites.take(1)
    private fun frame(watts: Double) = MeterFrame(mapOf(sites[0].id to watts), Instant.EPOCH)
    private fun source(meter: () -> Flow<MeterFrame>, model: () -> Flow<Double> = { flowOf(1.1) }) = object : TelemetrySource {
        override fun meter() = meter()
        override fun demandFactor() = model()
    }

    @Test fun coldSourceStartsOnlyWhenCollectedAndConvertsWatts() = runTest {
        var started = 0
        val stream = TelemetryEngine(sites, source({ flow { started++; emit(frame(2000.0)) } })).observe()
        assertEquals(0, started)
        val result = stream.toList().last()
        assertEquals(1, started)
        assertEquals(2.0, result.readings.single().measuredPowerKw, 0.0)
        assertEquals(17.6, result.readings.single().forecastDailyKwh, 1e-9)
    }

    @Test fun modelUpdatesRecalculateWithoutNewMeterSample() = runTest {
        val result = TelemetryEngine(sites, source({ flow { emit(frame(1000.0)); delay(100) } },
            { flow { emit(1.0); delay(10); emit(1.5) } })).observe().toList()
        assertEquals(12.0, result.last().readings.single().forecastDailyKwh, 0.0)
        assertEquals(Instant.EPOCH, result.last().readings.single().sampledAt)
    }

    @Test fun transientFailureEmitsRetryAndRecoversKeepingLastReading() = runTest {
        var collections = 0
        val engine = TelemetryEngine(sites, source({ flow {
            collections++
            if (collections == 1) { emit(frame(1000.0)); delay(1); throw IOException() }
            emit(frame(2000.0))
        } }), retryDelayMs = 100)
        val states = engine.observe().toList()
        val retry = states.first { it.phase == StreamPhase.RETRYING }
        assertEquals(1.0, retry.readings.single().measuredPowerKw, 0.0)
        assertEquals(StreamPhase.LIVE, states.last().phase)
        assertEquals(2.0, states.last().readings.single().measuredPowerKw, 0.0)
        assertEquals(2, collections)
    }

    @Test fun retriesAreBoundedAndBackoffIsApplied() = runTest {
        var attempts = 0
        val states = TelemetryEngine(sites, source({ flow { attempts++; throw IOException() } }), retryDelayMs = 100).observe().toList()
        assertEquals(4, attempts)
        assertEquals(600, testScheduler.currentTime)
        assertEquals(StreamPhase.FAILED, states.last().phase)
        assertEquals(3, states.last().retries)
    }

    @Test fun programmingFailureIsNotRetried() = runTest {
        var attempts = 0
        val states = TelemetryEngine(sites, source({ flow { attempts++; throw IllegalStateException() } })).observe().toList()
        assertEquals(1, attempts)
        assertEquals(StreamPhase.FAILED, states.last().phase)
    }

    @Test fun cancellationPropagatesAndDoesNotRetry() = runTest {
        var attempts = 0
        try {
            TelemetryEngine(sites, source({ flow { attempts++; throw CancellationException("stop") } })).observe().toList()
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { assertEquals(1, attempts) }
    }

    @Test fun invalidMeterFramesAreFiltered() = runTest {
        val result = TelemetryEngine(sites, source({ flowOf(frame(Double.NaN), frame(-1.0),
            MeterFrame(emptyMap(), Instant.EPOCH), frame(0.0)) })).observe().toList()
        assertEquals(0.0, result.last().readings.single().forecastDailyKwh, 0.0)
        assertFalse(result.any { it.readings.any { r -> !r.measuredPowerKw.isFinite() || r.measuredPowerKw < 0 } })
    }

    @Test fun modelFailureIsVisible() = runTest {
        val result = TelemetryEngine(sites, source({ flowOf(frame(1000.0)) }, { flow { throw IOException() } })).observe().toList()
        assertEquals(StreamPhase.FAILED, result.last().phase)
    }
}
