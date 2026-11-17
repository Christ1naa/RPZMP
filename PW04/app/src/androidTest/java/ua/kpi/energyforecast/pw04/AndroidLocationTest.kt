package ua.kpi.energyforecast.pw04

import android.Manifest
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ua.kpi.energyforecast.pw04.location.*

/** Реальний Android adapter + системний test provider, без Google Maps. */
class AndroidLocationTest {
    @get:Rule val compose = createComposeRule()

    @Suppress("DEPRECATION")
    @NeedsHostSetup @Test fun systemGpsDeliversTwoFreshLocationsAndDisabledStatus() = runBlocking {
        requireHostSetup()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val automation = instrumentation.uiAutomation
        fun shell(command: String) {
            automation.executeShellCommand(command).use { descriptor ->
                android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
            }
        }
        compose.setContent { Text("Перевірка foreground GPS") }
        automation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_COARSE_LOCATION)
        automation.grantRuntimePermission(context.packageName, Manifest.permission.ACCESS_FINE_LOCATION)
        assertTrue(hasLocationPermission(context))
        val manager = context.getSystemService(LocationManager::class.java)
        val out = Channel<LocationState>(Channel.UNLIMITED)
        var job: Job? = null
        var added = false
        shell("appops set ${context.packageName} android:mock_location allow")
        try {
            manager.addTestProvider("gps", false, false, false, false, true, true, true,
                Criteria.POWER_LOW, Criteria.ACCURACY_FINE)
            added = true
            manager.setTestProviderEnabled("gps", true)
            job = launch { LocationSource(context).observe().collect { out.send(it) } }
            withTimeout(5000) { out.receive() }
            fun send(latitude: Double) {
                manager.setTestProviderLocation("gps", Location("gps").apply {
                    this.latitude = latitude; longitude = 30.5234; accuracy = 5f
                    time = System.currentTimeMillis(); elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                })
            }
            suspend fun position(latitude: Double) = withTimeout(15000) {
                while (true) {
                    send(latitude)
                    val value = withTimeoutOrNull(1000) { out.receive() }
                    if (value?.position?.latitude == latitude) {
                        assertEquals("Поточна позиція", value.message)
                        assertEquals(5f, value.accuracyMeters)
                        break
                    }
                    delay(250)
                }
            }
            position(50.4501)
            delay(5500) // production request: 5 с / 10 м
            position(50.4601)
            manager.setTestProviderEnabled("gps", false)
            withTimeout(5000) {
                var value: LocationState
                do { value = out.receive() } while (!value.message.contains("вимкнено"))
                assertNull(value.position)
            }
        } finally {
            job?.cancelAndJoin()
            if (added) manager.removeTestProvider("gps")
            shell("appops set ${context.packageName} android:mock_location default")
        }
    }
}
