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
    private fun <T> ticking(value: T): Flow<T> = flow { while (true) { emit(value); delay(10) } }
    private fun source(meter: () -> Flow<MeterFrame> = { ticking(frame(1000.0)) },
                       model: () -> Flow<Double> = { ticking(1.1) }) = object : TelemetrySource {
        override fun meter() = meter()
        override fun demandFactor() = model()
    }
    private fun engine(source: TelemetrySource, retries: Int = 3) =
        TelemetryEngine(sites, source, retryDelayMs = 100, maxRetries = retries, dataTimeoutMs = 50)

    @Test fun coldSourceStartsOnlyWhenCollectedAndConvertsWatts() = runTest {
        var started = 0
        val stream = engine(source(meter = { flow { started++; emitAll(ticking(frame(2000.0))) } })).observe()
        assertEquals(0, started)
        val result = stream.first { it.phase == StreamPhase.LIVE }
        assertEquals(1, started)
        assertEquals(2.0, result.readings.single().measuredPowerKw, 0.0)
        assertEquals(17.6, result.readings.single().forecastDailyKwh, 1e-9)
    }

    @Test fun modelUpdatesRecalculateWithoutNewMeterSample() = runTest {
        val stream = engine(source(meter = { flow { emit(frame(1000.0)); awaitCancellation() } },
            model = { flow { emit(1.0); delay(10); emit(1.5); awaitCancellation() } })).observe()
        val result = stream.first { it.factor == 1.5 }
        assertEquals(12.0, result.readings.single().forecastDailyKwh, 0.0)
        assertEquals(Instant.EPOCH, result.readings.single().sampledAt)
    }

    @Test fun transientFailureEmitsRetryAndRecoversKeepingLastReading() = runTest {
        var collections = 0
        val states = engine(source(meter = { flow {
            collections++
            if (collections == 1) { emit(frame(1000.0)); delay(1); throw IOException() }
            emitAll(ticking(frame(2000.0)))
        } })).observe().takeWhile { it.phase != StreamPhase.LIVE || it.meterStatus.retries == 0 }.toList()
        val retry = states.first { it.meterStatus.phase == StreamPhase.RETRYING }
        assertEquals(1.0, retry.readings.single().measuredPowerKw, 0.0)
        assertTrue(retry.isStale)
        assertEquals(2, collections)
    }

    @Test fun meterRetriesAreBoundedAndBackoffIsApplied() = runTest {
        var attempts = 0
        val result = engine(source(meter = { attempts++; flow { throw IOException() } })).observe()
            .first { it.phase == StreamPhase.FAILED }
        assertEquals(4, attempts)
        assertEquals(600L, testScheduler.currentTime)
        assertEquals(3, result.meterStatus.retries)
        assertEquals(StreamIssue.IO, result.meterStatus.issue)
        assertEquals(StreamPhase.LIVE, result.forecastStatus.phase)
    }

    @Test fun forecastRetriesAreBoundedAndDoNotRestartMeter() = runTest {
        var attempts = 0
        var meterStarts = 0
        val result = engine(source(meter = { meterStarts++; ticking(frame(1000.0)) },
            model = { attempts++; flow { throw IOException() } })).observe().first { it.phase == StreamPhase.FAILED }
        assertEquals(4, attempts)
        assertEquals(1, meterStarts)
        assertEquals(600L, testScheduler.currentTime)
        assertEquals(3, result.forecastStatus.retries)
        assertEquals(StreamPhase.LIVE, result.meterStatus.phase)
    }

    @Test fun forecastFailureRecoversWithoutRestartingMeter() = runTest {
        var attempts = 0
        var meterStarts = 0
        val states = mutableListOf<TelemetryState>()
        val result = engine(source(meter = { meterStarts++; ticking(frame(1000.0)) }, model = {
            attempts++
            flow { if (attempts == 1) { emit(1.0); delay(1); throw IOException() }; emitAll(ticking(1.5)) }
        })).observe().onEach { states += it }.first { it.phase == StreamPhase.LIVE && it.forecastStatus.retries == 1 }
        assertEquals(2, attempts)
        assertEquals(1, meterStarts)
        assertEquals(12.0, result.readings.single().forecastDailyKwh, 0.0)
        assertTrue(states.any { it.forecastStatus.phase == StreamPhase.RETRYING && it.isStale })
        assertFalse(result.isStale)
    }

    @Test fun lastCompleteForecastIsFrozenWhileEitherSourceRecovers() = runTest {
        val states = mutableListOf<TelemetryState>()
        engine(source(meter = { flow { emit(frame(1000.0)); delay(1); throw IOException() } },
            model = { flow { emit(1.0); delay(5); emitAll(ticking(1.8)) } }))
            .observe().onEach { states += it }.first { it.factor == 1.8 && it.phase == StreamPhase.LIVE }
        val stale = states.filter { it.isStale }
        assertTrue(stale.isNotEmpty())
        assertTrue(stale.all { it.factor == 1.0 && it.readings.single().forecastDailyKwh == 8.0 })
    }

    @Test fun programmingFailureIsNotRetriedForEitherSource() = runTest {
        for (forecast in listOf(false, true)) {
            var attempts = 0
            val bad: () -> Flow<Double> = { attempts++; flow { throw IllegalStateException() } }
            val src = if (forecast) source(model = bad) else source(meter = { attempts++; flow { throw IllegalStateException() } })
            val result = engine(src).observe().first { it.phase == StreamPhase.FAILED }
            assertEquals(1, attempts)
            assertEquals(StreamIssue.SOURCE_ERROR, (if (forecast) result.forecastStatus else result.meterStatus).issue)
        }
    }

    @Test fun cancellationFromEitherSourcePropagatesAndDoesNotRetry() = runTest {
        for (forecast in listOf(false, true)) {
            var attempts = 0
            val src = if (forecast) source(model = { attempts++; flow { throw CancellationException("stop") } })
                else source(meter = { attempts++; flow { throw CancellationException("stop") } })
            try {
                engine(src).observe().collect()
                fail("Cancellation must propagate")
            } catch (_: CancellationException) { assertEquals(1, attempts) }
        }
    }

    @Test fun invalidMeterFramesAreFilteredAndZeroIsValid() = runTest {
        val result = engine(source(meter = { flow {
            emit(frame(Double.NaN)); emit(frame(-1.0)); emit(MeterFrame(emptyMap(), Instant.EPOCH))
            emit(frame(0.0)); awaitCancellation()
        } })).observe().first { it.phase == StreamPhase.LIVE }
        assertEquals(0.0, result.readings.single().forecastDailyKwh, 0.0)
        assertFalse(result.isStale)
    }

    @Test fun emptyMeterIsRetriedAndFailsWithoutFalseZeroForecast() = runTest {
        var attempts = 0
        val result = engine(source(meter = { attempts++; emptyFlow() })).observe().first { it.phase == StreamPhase.FAILED }
        assertEquals(4, attempts)
        assertEquals(StreamIssue.NO_DATA, result.meterStatus.issue)
        assertTrue(result.readings.isEmpty())
        assertNull(result.factor)
    }

    @Test fun silentMeterTimesOutAndCancelsEachOldProducer() = runTest {
        var starts = 0
        var stops = 0
        val result = engine(source(meter = { flow {
            starts++
            try { awaitCancellation() } finally { stops++ }
        } })).observe().first { it.phase == StreamPhase.FAILED }
        assertEquals(4, starts)
        assertEquals(4, stops)
        assertEquals(800L, testScheduler.currentTime) // 4 × 50 ms + (100 + 200 + 300).
        assertEquals(StreamIssue.NO_DATA, result.meterStatus.issue)
    }

    @Test fun invalidMeterTrafficDoesNotResetTimeout() = runTest {
        val result = engine(source(meter = { ticking(frame(Double.NaN)) }), retries = 0)
            .observe().first { it.phase == StreamPhase.FAILED }
        assertEquals(50L, testScheduler.currentTime)
        assertEquals(StreamIssue.NO_DATA, result.meterStatus.issue)
    }

    @Test fun silentForecastTimesOutIndependently() = runTest {
        val result = engine(source(model = { flow { awaitCancellation() } }), retries = 0)
            .observe().first { it.phase == StreamPhase.FAILED }
        assertEquals(50L, testScheduler.currentTime)
        assertEquals(StreamIssue.NO_DATA, result.forecastStatus.issue)
        assertEquals(StreamPhase.LIVE, result.meterStatus.phase)
    }

    @Test fun emptyAndInvalidForecastsAreNoDataErrors() = runTest {
        for (flow in listOf(emptyFlow(), flowOf(Double.NaN, Double.POSITIVE_INFINITY, -1.0, 3.0))) {
            val result = engine(source(model = { flow }), retries = 0).observe().first { it.phase == StreamPhase.FAILED }
            assertEquals(StreamIssue.NO_DATA, result.forecastStatus.issue)
        }
    }

    @Test fun silenceAfterValidReadingBecomesStaleAtDeadline() = runTest {
        val states = mutableListOf<TelemetryState>()
        val result = engine(source(meter = { flow { emit(frame(1000.0)); awaitCancellation() } }), retries = 0)
            .observe().onEach { states += it }.first { it.phase == StreamPhase.FAILED }
        assertTrue(states.any { it.phase == StreamPhase.LIVE })
        assertEquals(50L, testScheduler.currentTime)
        assertTrue(result.isStale)
        assertEquals(8.8, result.readings.single().forecastDailyKwh, 1e-9)
    }

    @Test fun regularValidDataKeepsResettingTheWatchdog() = runTest {
        val states = mutableListOf<TelemetryState>()
        val job = backgroundScope.launch { engine(source()).observe().collect { states += it } }
        advanceTimeBy(201); runCurrent()
        assertTrue(states.count { it.phase == StreamPhase.LIVE } > 5)
        assertFalse(states.any { it.phase == StreamPhase.RETRYING || it.phase == StreamPhase.FAILED })
        job.cancelAndJoin()
    }

    @Test fun cancellingDuringBackoffStopsBothSourcesWithoutAnotherAttempt() = runTest {
        var attempts = 0
        var forecastStops = 0
        val job = backgroundScope.launch { engine(source(meter = { attempts++; flow { throw IOException() } }, model = {
            flow { try { emit(1.1); awaitCancellation() } finally { forecastStops++ } }
        })).observe().collect() }
        runCurrent()
        assertEquals(1, attempts)
        job.cancelAndJoin()
        advanceTimeBy(1000); runCurrent()
        assertEquals(1, attempts)
        assertEquals(1, forecastStops)
    }

    @Test fun retryBudgetIsPerSessionAndDoesNotResetOnOneGoodSample() = runTest {
        var attempts = 0
        val result = engine(source(meter = { attempts++; flow { emit(frame(1000.0)); delay(1); throw IOException() } }))
            .observe().first { it.phase == StreamPhase.FAILED }
        assertEquals(4, attempts)
        assertTrue(result.isStale)
        assertEquals(3, result.meterStatus.retries)
    }

    @Test fun bothFailuresRemainVisibleAndFailedTakesPriority() = runTest {
        val result = engine(source(meter = { flow { throw IllegalStateException() } },
            model = { flow { throw IOException() } })).observe()
            .first { it.meterStatus.phase == StreamPhase.FAILED && it.forecastStatus.phase == StreamPhase.RETRYING }
        assertEquals(StreamPhase.FAILED, result.phase)
        assertEquals(StreamIssue.SOURCE_ERROR, result.meterStatus.issue)
        assertEquals(StreamIssue.IO, result.forecastStatus.issue)
    }

    @Test fun invalidWatchdogAndRetrySettingsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { TelemetryEngine(sites, source(), dataTimeoutMs = 0) }
        assertThrows(IllegalArgumentException::class.java) { TelemetryEngine(sites, source(), retryDelayMs = -1) }
        assertThrows(IllegalArgumentException::class.java) { TelemetryEngine(sites, source(), maxRetries = 11) }
    }
}
