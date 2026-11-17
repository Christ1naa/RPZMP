package ua.kpi.energyforecast.pw04

import android.Manifest
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Кожен метод запускається host-скриптом із явно заданим permission state. */
class DevicePermissionTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun sites() { compose.onNodeWithText("Об'єкти й мапа").performClick() }
    private fun granted(permission: String) = ContextCompat.checkSelfPermission(compose.activity, permission) == PackageManager.PERMISSION_GRANTED
    private fun dialog() {
        val until = SystemClock.uptimeMillis() + 10000
        while (SystemClock.uptimeMillis() < until) {
            if (findSystemNode("While using the app") != null || findSystemNode("While using this app") != null || findSystemNode("Only this time") != null) return
            SystemClock.sleep(100)
        }
        error("Permission dialog did not appear")
    }
    private fun allow() = clickSystemText("While using the app", "While using this app", "Allow only while using the app")

    @NeedsHostSetup @Test fun precisePermissionThroughSystemDialog() {
        requireHostSetup()
        sites(); compose.onNodeWithText("Моя позиція").performClick(); dialog()
        if (findSystemNode("Precise") != null) clickSystemText("Precise")
        captureVerificationScreen("10-permission-precise-dialog")
        allow()
        compose.waitUntil(5000) { granted(Manifest.permission.ACCESS_FINE_LOCATION) }
        assertTrue(granted(Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    @NeedsHostSetup @Test fun approximatePermissionThroughSystemDialog() {
        requireHostSetup()
        sites(); compose.onNodeWithText("Моя позиція").performClick(); dialog()
        clickSystemText("Approximate")
        captureVerificationScreen("11-permission-approximate-dialog")
        allow()
        compose.waitUntil(5000) { granted(Manifest.permission.ACCESS_COARSE_LOCATION) }
        assertFalse(granted(Manifest.permission.ACCESS_FINE_LOCATION))
    }
    @NeedsHostSetup @Test fun denyTwiceThenOpenOwnSettingsAndReturn() {
        requireHostSetup()
        sites(); compose.onNodeWithText("Моя позиція").performClick(); dialog()
        captureVerificationScreen("12-permission-denial-dialog")
        clickSystemText("Don't allow", "Don’t allow", "Deny")
        compose.onNodeWithText("Немає дозволу на геолокацію. Мапа об'єктів доступна.").assertIsDisplayed()
        captureVerificationScreen("13-permission-denied-app")
        compose.onNodeWithText("Моя позиція").performClick(); dialog()
        clickSystemText("Don't allow", "Don’t allow", "Deny")
        compose.waitForIdle()
        assertFalse(granted(Manifest.permission.ACCESS_COARSE_LOCATION))
        assertFalse(ActivityCompat.shouldShowRequestPermissionRationale(compose.activity, Manifest.permission.ACCESS_FINE_LOCATION))
        compose.onNodeWithText("Моя позиція").performClick(); compose.waitForIdle()
        SystemClock.sleep(500)
        assertEquals(compose.activity.packageName,
            InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow?.packageName?.toString())
        captureVerificationScreen("14-permission-permanently-denied-app")
        compose.onNodeWithText("Дозволи").performClick()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val until = SystemClock.uptimeMillis() + 5000
        while (SystemClock.uptimeMillis() < until && automation.rootInActiveWindow?.packageName?.toString() != "com.android.settings") SystemClock.sleep(100)
        assertEquals("com.android.settings", automation.rootInActiveWindow?.packageName?.toString())
        val renderedUntil = SystemClock.uptimeMillis() + 5000
        while (SystemClock.uptimeMillis() < renderedUntil && findSystemNode("Permissions") == null) SystemClock.sleep(100)
        assertNotNull("Own app info content rendered", findSystemNode("Permissions"))
        captureVerificationScreen("15-own-app-settings")
        automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        compose.onNodeWithText("Немає дозволу на геолокацію. Мапа об'єктів доступна.").assertIsDisplayed()
    }
    @NeedsHostSetup @Test fun revokedPermissionIsRecheckedOnNewActivity() {
        requireHostSetup()
        sites()
        assertFalse(granted(Manifest.permission.ACCESS_COARSE_LOCATION))
        compose.onNodeWithText("Немає дозволу на геолокацію. Мапа об'єктів доступна.").assertIsDisplayed()
        captureVerificationScreen("16-permission-revoked-app")
    }
    @NeedsHostSetup @Test fun locationDisabledAndReturnFromGpsSettings() {
        requireHostSetup()
        sites()
        compose.onNodeWithText("Геолокацію вимкнено. Увімкніть GPS або мережеве визначення.").assertIsDisplayed()
        captureVerificationScreen("17-location-disabled")
        compose.onNodeWithText("GPS").performClick()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val until = SystemClock.uptimeMillis() + 5000
        while (SystemClock.uptimeMillis() < until && automation.rootInActiveWindow?.packageName?.toString() != "com.android.settings") SystemClock.sleep(100)
        assertEquals("com.android.settings", automation.rootInActiveWindow?.packageName?.toString())
        verificationShell("cmd location set-location-enabled true")
        automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
        compose.waitUntil(10000) {
            compose.onAllNodesWithText("Геолокацію вимкнено. Увімкніть GPS або мережеве визначення.").fetchSemanticsNodes().isEmpty()
        }
        // Не зберігаємо карту фактичного приватного місцезнаходження.
        compose.onNodeWithTag("map-toggle").performClick()
        compose.onNodeWithText("Об'єкти споживання").assertIsDisplayed()
    }
}
