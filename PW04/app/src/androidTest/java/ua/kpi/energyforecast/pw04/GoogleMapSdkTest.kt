package ua.kpi.energyforecast.pw04

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import ua.kpi.energyforecast.pw04.data.DemoSites
import ua.kpi.energyforecast.pw04.location.LocationState
import ua.kpi.energyforecast.pw04.model.SiteReading
import ua.kpi.energyforecast.pw04.ui.EnergyMap
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import java.io.File
import android.graphics.Bitmap

/** Це справжній SDK, а не заміна мапи. Без ключа тести ЯВНО пропускаються. */
class GoogleMapSdkTest {
    @Before fun keepScreenAwake() = keepMapsTestScreenAwake()
    @get:Rule val compose = createComposeRule()
    private val readings = DemoSites.sites.map { SiteReading(it, 1.0, 8.0, Instant.EPOCH) }

    private fun requireMaps() {
        assumeTrue("Потрібен локальний MAPS_API_KEY та перебудова APK", BuildConfig.MAPS_CONFIGURED)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assumeTrue("Потрібні Google Play services", GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS)
    }

    private fun assertTarget(map: GoogleMap, expected: LatLng) {
        compose.runOnIdle {
            assertEquals(expected.latitude, map.cameraPosition.target.latitude, 0.00001)
            assertEquals(expected.longitude, map.cameraPosition.target.longitude, 0.00001)
        }
    }

    private fun captureEvidence(name: String, sdk: GoogleMap) {
        compose.waitForIdle()
        val completed = AtomicBoolean(false)
        // SDK snapshot is queued after camera updates; capture the map renderer itself.
        compose.runOnIdle {
            sdk.snapshot { bitmap ->
                checkNotNull(bitmap)
                val instrumentation = InstrumentationRegistry.getInstrumentation()
                val directory = File(instrumentation.targetContext.filesDir, "pw04-maps-evidence").apply { mkdirs() }
                File(directory, "$name.png").outputStream().use {
                    check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
                }
                bitmap.recycle()
                completed.set(true)
            }
        }
        compose.waitUntil(10000) { completed.get() }
    }

    @NeedsMapsKey @Test fun realCameraRefocusesSameIdAfterPanButNotOnNewReading() {
        requireMaps()
        val loaded = AtomicBoolean(false)
        var sdk: GoogleMap? = null
        var revision by mutableStateOf(1L)
        var data by mutableStateOf(readings)
        val home = readings.first().site.position.let { LatLng(it.latitude, it.longitude) }
        compose.setContent {
            EnergyMap(data, "home-1", LocationState(), 0, {}, Modifier.fillMaxSize(),
                selectionRevision = revision, onMapAvailable = { sdk = it }, onLoaded = { loaded.set(true) })
        }
        compose.waitUntil(timeoutMillis = 45000) { loaded.get() }
        compose.waitForIdle()
        assertTarget(sdk!!, home)
        val pan = LatLng(50.60, 30.70)
        compose.runOnIdle { sdk!!.moveCamera(CameraUpdateFactory.newLatLng(pan)) }
        compose.runOnIdle { data = readings.map { it.copy(forecastDailyKwh = 99.0) } }
        compose.waitForIdle()
        assertTarget(sdk!!, pan)
        compose.runOnIdle { revision++ }
        compose.waitForIdle()
        assertTarget(sdk!!, home)
        captureEvidence("03-camera-refocused", sdk!!)
    }

    @NeedsMapsKey @Test fun realMarkerTouchSelectsItsAssociatedIdAndFocusesCamera() {
        requireMaps()
        val loaded = AtomicBoolean(false)
        var sdk: GoogleMap? = null
        var selected by mutableStateOf<String?>(null)
        var revision by mutableStateOf(0L)
        val site = DemoSites.sites.first { it.id == "office-2" }
        val target = LatLng(site.position.latitude, site.position.longitude)
        compose.setContent {
            EnergyMap(readings, selected, LocationState(), 0, { id -> selected = id; revision++ }, Modifier.fillMaxSize(),
                selectionRevision = revision, onMapAvailable = { sdk = it }, onLoaded = { loaded.set(true) })
        }
        compose.waitUntil(timeoutMillis = 45000) { loaded.get() }
        captureEvidence("01-map-loaded", sdk!!)
        var point = android.graphics.Point()
        compose.runOnIdle { point = sdk!!.projection.toScreenLocation(target) }
        compose.onNodeWithTag("google-map").performTouchInput { click(Offset(point.x.toFloat(), point.y - 12f)) }
        compose.waitUntil(timeoutMillis = 5000) { selected == site.id }
        compose.waitForIdle()
        assertTarget(sdk!!, target)
        captureEvidence("02-marker-selected", sdk!!)
    }
}
