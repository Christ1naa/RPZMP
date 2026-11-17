package ua.kpi.energyforecast.pw04

import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ua.kpi.energyforecast.pw04.location.*

class DeviceLocationEvidenceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @NeedsHostSetup @Test fun freshRealDeviceFixWithoutSavingCoordinates() = runBlocking {
        requireHostSetup()
        val sample = withTimeout(45000) { LocationSource(compose.activity).observe().first { it.position != null } }
        assertNotNull(sample.position)
        assertFalse("Real source must not be a mock provider", sample.isMock)
        assertNotNull(sample.provider)
        InstrumentationRegistry.getInstrumentation().sendStatus(2, Bundle().apply {
            putString("pw04_real_location_provider", sample.provider)
            putString("pw04_real_location_is_mock", sample.isMock.toString())
            putString("pw04_real_location_accuracy_present", (sample.accuracyMeters != null).toString())
        })
        // Координати не логуються, не зберігаються й не потрапляють у screenshots.
    }

    @Suppress("DEPRECATION")
    @NeedsHostSetup @Test fun labelledSyntheticPositionUpdatesAndProvidersRecover() = runBlocking {
        requireHostSetup()
        val manager = compose.activity.getSystemService(LocationManager::class.java)
        val pkg = compose.activity.packageName
        val providers = listOf("gps", "network")
        val added = mutableListOf<String>()
        val out = Channel<LocationState>(Channel.UNLIMITED)
        var job: Job? = null
        verificationShell("appops set $pkg android:mock_location allow")
        try {
            providers.forEach { provider ->
                manager.addTestProvider(provider, false, false, false, false, true, true, true,
                    Criteria.POWER_LOW, Criteria.ACCURACY_FINE)
                added += provider; manager.setTestProviderEnabled(provider, true)
            }
            fun send(lat: Double, lon: Double, accuracy: Float) {
                providers.forEach { provider -> manager.setTestProviderLocation(provider, Location(provider).apply {
                    latitude = lat; longitude = lon; this.accuracy = accuracy
                    time = System.currentTimeMillis(); elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                }) }
            }
            send(50.4511, 30.5248, 5f)
            job = launch(Dispatchers.Default) { LocationSource(compose.activity).observe().collect { out.send(it) } }
            withTimeout(5000) { out.receive() }
            compose.onNodeWithText("Об'єкти й мапа").performClick()
            compose.onNodeWithText("Моя позиція").performClick()
            suspend fun awaitFix(lat: Double, lon: Double, accuracy: Float) {
                withTimeout(15000) {
                    while (true) {
                        send(lat, lon, accuracy)
                        val value = withTimeoutOrNull(600) { out.receive() }
                        if (value?.position?.latitude == lat && value?.position?.longitude == lon) {
                            assertTrue(value.isMock); break
                        }
                        delay(100)
                    }
                }
                compose.waitUntil(15000) {
                    send(lat, lon, accuracy)
                    compose.onAllNodesWithText("±${accuracy.toInt()} м", substring = true).fetchSemanticsNodes().isNotEmpty()
                }
                compose.onNodeWithTag("location-status").assertTextContains("тестова позиція", substring = true)
            }
            awaitFix(50.4511, 30.5248, 5f)
            captureVerificationScreen("30-labelled-synthetic-position")
            delay(5500)
            awaitFix(50.4518, 30.5258, 6f)
            captureVerificationScreen("31-labelled-position-updated")
            providers.forEach { manager.setTestProviderEnabled(it, false) }
            compose.waitUntil(10000) {
                compose.onAllNodesWithText("Геолокацію вимкнено.", substring = true).fetchSemanticsNodes().isNotEmpty()
            }
            captureVerificationScreen("32-providers-disabled")
            providers.forEach { manager.setTestProviderEnabled(it, true) }
            awaitFix(50.4518, 30.5258, 7f)
            captureVerificationScreen("33-providers-recovered")
        } finally {
            job?.cancelAndJoin()
            added.forEach { manager.removeTestProvider(it) }
            verificationShell("appops set $pkg android:mock_location default")
        }
    }
}
