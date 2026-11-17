package ua.kpi.energyforecast.pw04

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ua.kpi.energyforecast.pw04.location.LocationState
import ua.kpi.energyforecast.pw04.ui.EnergyMap
import java.util.concurrent.atomic.AtomicBoolean

/** Запускається detached host-скриптом останнім: Wi-Fi adb тимчасово від'єднається. */
class NetworkRecoveryTest {
    @get:Rule val compose = createComposeRule()
    @NeedsHostSetup @Test fun realNetworkLossThenRetryAndRestore() {
        requireHostSetup(networkDisruption = true)
        val wifi = verificationShell("settings get global wifi_on").trim()
        val data = verificationShell("settings get global mobile_data").trim()
        assertTrue("This scenario requires initially enabled Wi-Fi", wifi == "1")
        val loaded = AtomicBoolean(false)
        fun restoreNetwork() {
            verificationShell(if (data == "1") "svc data enable" else "svc data disable")
            verificationShell("svc wifi enable")
        }
        try {
            verificationShell("svc data disable")
            verificationShell("svc wifi disable")
            android.os.SystemClock.sleep(2500)
            assertEquals("0", verificationShell("settings get global wifi_on").trim())
            compose.setContent {
                var generation by remember { mutableIntStateOf(0) }
                MaterialTheme { key(generation) {
                    EnergyMap(emptyList(), null, LocationState(), 0, {}, Modifier.fillMaxSize(),
                        onRetry = { generation++ }, onLoaded = { loaded.set(true) },
                        // Uncached remote test area; not the user's physical position.
                        initialCameraPosition = CameraPosition.fromLatLngZoom(LatLng(64.1466, -21.9426), 19f))
                } }
            }
            // LaunchedEffect delay uses the Compose test scheduler, not wall-clock time.
            compose.mainClock.advanceTimeBy(20_100)
            compose.waitUntil(45000) { compose.onAllNodesWithText("Повторити мапу").fetchSemanticsNodes().isNotEmpty() }
            assertFalse(loaded.get())
            captureVerificationScreen("50-network-off-map-failed")
            compose.onNodeWithText("Повторити мапу").performClick()
            compose.onNodeWithText("Завантаження мапи… Перевірте мережу та ключ Google Maps.").assertIsDisplayed()
            captureVerificationScreen("51-network-retry-loading")
            restoreNetwork()
            compose.waitUntil(90000) { loaded.get() }
            compose.onNodeWithTag("google-map").assertIsDisplayed()
            compose.onNodeWithTag("map-load-status").assertDoesNotExist()
            captureVerificationScreen("52-network-restored-map-loaded")
        } finally {
            restoreNetwork()
            if (verificationShell("settings get global wifi_on").trim() == wifi &&
                verificationShell("settings get global mobile_data").trim() == data) {
                verificationShell("touch /data/local/tmp/pw04-network-restored")
            }
        }
    }
}
