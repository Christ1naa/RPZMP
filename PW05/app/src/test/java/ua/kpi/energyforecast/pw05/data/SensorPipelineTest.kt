package ua.kpi.energyforecast.pw05.data

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import ua.kpi.energyforecast.pw05.domain.*
import ua.kpi.energyforecast.pw05.ui.SensorViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class SensorPipelineTest {
    private class Fake : SensorBackend {
        var present = true
        var succeeds = true
        val callbacks = mutableMapOf<SensorKind, (Long, FloatArray) -> Unit>()
        var starts = 0
        var stops = 0
        override fun available(kind: SensorKind) = present
        override fun register(kind: SensorKind, onReading: (Long, FloatArray) -> Unit): (() -> Unit)? {
            if (!succeeds) return null
            starts++; callbacks[kind] = onReading
            return { stops++; callbacks.remove(kind); Unit }
        }
    }
    @Test fun vectorsAreValidatedAndCopied() {
        val values = floatArrayOf(3f,4f,0f)
        val reading = requireNotNull(ReadingValidator.parse(SensorKind.ACCELEROMETER,1,values))
        values[0] = 100f
        assertEquals(5f,reading.magnitude,0f)
        assertEquals(3f,reading.values[0],0f)
        assertNull(ReadingValidator.parse(SensorKind.LIGHT,0,floatArrayOf(1f)))
        assertNull(ReadingValidator.parse(SensorKind.LIGHT,1,floatArrayOf(-1f)))
        assertNull(ReadingValidator.parse(SensorKind.GYROSCOPE,1,floatArrayOf(1f)))
        assertNull(ReadingValidator.parse(SensorKind.LIGHT,1,floatArrayOf(Float.NaN)))
        assertNull(ReadingValidator.parse(SensorKind.LIGHT,1,floatArrayOf(Float.POSITIVE_INFINITY)))
    }
    @Test fun queryUsesInclusiveBoundsAndNumericOrder() {
        val rows = listOf(1f,10f,2f).mapIndexed { i,v -> requireNotNull(ReadingValidator.parse(SensorKind.LIGHT,i+1L,floatArrayOf(v))) }
        assertEquals(listOf(2f,10f),SensorQuery(SensorKind.LIGHT,2f,10f,SensorSort.VALUE,false).apply(rows).map { it.magnitude })
        assertTrue(SensorQuery(SensorKind.GYROSCOPE).apply(rows).isEmpty())
        assertEquals(listOf(3L,2L,1L),SensorQuery().apply(rows).map { it.timestampNanos })
        assertThrows(IllegalArgumentException::class.java) { SensorQuery(minimum=3f,maximum=2f) }
        assertThrows(IllegalArgumentException::class.java) { SensorQuery(maximum=Float.NaN) }
    }
    @Test fun thresholdsHaveHysteresis() {
        val gate=ThresholdGate()
        assertFalse(gate.update(10f,10f)); assertTrue(gate.update(11f,10f))
        assertFalse(gate.update(12f,10f)); assertFalse(gate.update(9.8f,10f))
        assertFalse(gate.update(11f,10f)); assertFalse(gate.update(9f,10f))
        assertTrue(gate.update(11f,10f))
    }
    @Test fun missingAndFailedStatesAreExplicit() = runTest {
        val fake=Fake(); fake.present=false
        assertEquals(SensorStatus.MISSING,SensorRepository(fake).observe(SensorKind.LIGHT).last().status)
        fake.present=true; fake.succeeds=false
        assertEquals(SensorStatus.FAILED,SensorRepository(fake).observe(SensorKind.LIGHT).last().status)
    }
    @Test fun boundedHistoryRejectsInvalidAndThrottlesFastEvents() = runTest {
        val fake=Fake(); var latest: SensorSnapshot?=null
        val job=launch { SensorRepository(fake).observe(SensorKind.LIGHT).collect { latest=it } }; runCurrent()
        repeat(130) { i -> fake.callbacks.getValue(SensorKind.LIGHT)((i+1L)*100_000_000,floatArrayOf(i.toFloat())); runCurrent() }
        assertEquals(120,latest!!.readings.size); assertEquals(10f,latest!!.readings.first().magnitude,0f)
        fake.callbacks.getValue(SensorKind.LIGHT)(13_010_000_000,floatArrayOf(999f)); runCurrent()
        assertEquals(129f,latest!!.readings.last().magnitude,0f)
        fake.callbacks.getValue(SensorKind.LIGHT)(1,floatArrayOf(Float.NaN)); runCurrent()
        assertEquals(SensorStatus.INVALID,latest!!.status)
        assertEquals(129f,latest!!.readings.last().magnitude,0f)
        job.cancelAndJoin(); assertEquals(fake.starts,fake.stops)
    }
    @Test fun visibleCollectorsControlSubscriptionsAndSelection() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val fake=Fake(); val model=SensorViewModel(SensorRepository(fake))
            assertTrue(fake.callbacks.isEmpty())
            val job=launch { model.state.collect() }; runCurrent()
            assertEquals(SensorKind.entries.toSet(),fake.callbacks.keys)
            model.select(SensorKind.LIGHT,false); runCurrent()
            assertFalse(SensorKind.LIGHT in fake.callbacks)
            model.threshold(SensorKind.GYROSCOPE,4f); runCurrent()
            assertEquals(4f,model.state.value.settings.thresholds.getValue(SensorKind.GYROSCOPE),0f)
            job.cancelAndJoin(); runCurrent()
            assertTrue(fake.callbacks.isEmpty()); assertEquals(fake.starts,fake.stops)
            val again=launch { model.state.collect() }; runCurrent()
            assertEquals(2,fake.callbacks.size)
            again.cancelAndJoin(); runCurrent()
            assertTrue(fake.callbacks.isEmpty())
        } finally { Dispatchers.resetMain() }
    }

    @Test fun staleStateReturnsToLiveAndCancellationStopsTimer() = runTest {
        val fake = Fake()
        var latest: SensorSnapshot? = null
        val job = launch { SensorRepository(fake).observe(SensorKind.LIGHT).collect { latest = it } }
        runCurrent()
        fake.callbacks.getValue(SensorKind.LIGHT)(100_000_000, floatArrayOf(10f))
        runCurrent()
        assertEquals(SensorStatus.LIVE, latest!!.status)
        advanceTimeBy(5001); runCurrent()
        assertEquals(SensorStatus.STALE, latest!!.status)
        assertEquals(10f, latest!!.readings.last().magnitude, 0f)
        fake.callbacks.getValue(SensorKind.LIGHT)(200_000_000, floatArrayOf(11f))
        runCurrent()
        assertEquals(SensorStatus.LIVE, latest!!.status)
        job.cancelAndJoin()
        advanceTimeBy(6000); runCurrent()
        assertEquals(fake.starts, fake.stops)
    }
    @Test fun thresholdEventsAreSharedAcrossCollectorsAndSettingsArePersisted() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val fake = Fake()
            var events = 0
            var saved: ua.kpi.energyforecast.pw05.ui.SensorSettings? = null
            val model = SensorViewModel(SensorRepository(fake),
                persist = { saved = it }, notify = { _, _, _ -> events++ })
            val first = launch { model.state.collect() }
            val second = launch { model.state.collect() }
            runCurrent()
            assertEquals(3, fake.starts)
            model.threshold(SensorKind.LIGHT, 10f)
            runCurrent()
            assertEquals(10f, saved!!.thresholds.getValue(SensorKind.LIGHT), 0f)
            listOf(11f, 12f, 9.8f, 9f, 11f).forEachIndexed { i, v ->
                fake.callbacks.getValue(SensorKind.LIGHT)((i+1L)*100_000_000, floatArrayOf(v))
                runCurrent()
            }
            assertEquals(2, events)
            first.cancelAndJoin(); second.cancelAndJoin(); runCurrent()
            assertEquals(fake.starts, fake.stops)
        } finally { Dispatchers.resetMain() }
    }
}
