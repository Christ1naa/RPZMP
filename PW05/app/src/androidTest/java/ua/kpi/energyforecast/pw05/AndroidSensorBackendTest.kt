package ua.kpi.energyforecast.pw05

import android.content.Context
import android.hardware.SensorManager
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import ua.kpi.energyforecast.pw05.data.*
import ua.kpi.energyforecast.pw05.domain.*

class AndroidSensorBackendTest {
    @Test fun platformAvailabilityAndStreamsAreConsistent() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val backend = AndroidSensorBackend(context.getSystemService(Context.SENSOR_SERVICE) as SensorManager)
        for (kind in SensorKind.entries) {
            val present = backend.available(kind)
            val snapshot = withTimeout(3000) { SensorRepository(backend).observe(kind).first() }
            if (!present) assertEquals(SensorStatus.MISSING, snapshot.status)
            else assertTrue(snapshot.status in setOf(SensorStatus.WAITING, SensorStatus.LIVE, SensorStatus.FAILED))
        }
    }

    @Test fun twoAvailableSensorsProduceValidatedLiveReadings() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val backend = AndroidSensorBackend(context.getSystemService(Context.SENSOR_SERVICE) as SensorManager)
        val available = listOf(SensorKind.ACCELEROMETER, SensorKind.GYROSCOPE).filter { backend.available(it) }
        org.junit.Assume.assumeTrue("Потрібні два датчики", available.size == 2)
        for (kind in available) {
            val live = withTimeout(8000) { SensorRepository(backend).observe(kind).first { it.status == SensorStatus.LIVE } }
            assertTrue(live.readings.last().magnitude.isFinite())
            assertEquals(3, live.readings.last().values.size)
        }
    }
}
