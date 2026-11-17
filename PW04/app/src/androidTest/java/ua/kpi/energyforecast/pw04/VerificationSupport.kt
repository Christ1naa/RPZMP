package ua.kpi.energyforecast.pw04

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import android.view.WindowManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.runner.Description
import org.junit.runner.Result
import org.junit.runner.notification.RunListener
import java.io.File

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class NeedsMapsKey
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class NeedsNoMapsKey
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class NeedsHostSetup

/** Listener для всієї серії; лише тестові вікна, не системний screen timeout. */
class KeepAwakeListener : RunListener() {
    private val callback = ActivityLifecycleCallback { activity, stage ->
        if (stage == Stage.RESUMED) activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    override fun testRunStarted(description: Description) {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.serviceInfo = automation.serviceInfo.apply {
            flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            ActivityLifecycleMonitorRegistry.getInstance().addLifecycleCallback(callback)
        }
    }
    override fun testRunFinished(result: Result) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            ActivityLifecycleMonitorRegistry.getInstance().removeLifecycleCallback(callback)
        }
    }
}

internal fun verificationShell(command: String): String {
    val fd = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
    return android.os.ParcelFileDescriptor.AutoCloseInputStream(fd).use { it.readBytes().toString(Charsets.UTF_8) }
}

internal fun captureVerificationScreen(name: String) {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val automation = instrumentation.uiAutomation
    instrumentation.runOnMainSync {
        ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).forEach { activity ->
            activity.getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                .hideSoftInputFromWindow(activity.currentFocus?.windowToken ?: activity.window.decorView.windowToken, 0)
        }
    }
    val allowed = setOf(instrumentation.targetContext.packageName,
        "com.google.android.permissioncontroller", "com.android.permissioncontroller", "com.android.settings")
    var visiblePackage = automation.rootInActiveWindow?.packageName?.toString().orEmpty()
    val deadline = SystemClock.uptimeMillis() + 4000
    while (visiblePackage !in allowed && SystemClock.uptimeMillis() < deadline) {
        SystemClock.sleep(50)
        visiblePackage = automation.rootInActiveWindow?.packageName?.toString().orEmpty()
    }
    check(visiblePackage in allowed) {
        "Refusing to capture a window outside PW04 or its system permission/settings flow"
    }
    // Assertions inspect semantics before SurfaceFlinger necessarily presents the frame.
    // Allow keyboard/window animations to finish before collecting original pixels.
    instrumentation.waitForIdleSync()
    try { automation.waitForIdle(250, 2000) } catch (_: java.util.concurrent.TimeoutException) { }
    SystemClock.sleep(650)
    val bitmap = checkNotNull(automation.takeScreenshot())
    val directory = File(instrumentation.targetContext.filesDir, "pw04-final-evidence").apply { mkdirs() }
    File(directory, "$name.png").outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    bitmap.recycle()
}

internal fun findSystemNode(text: String): AccessibilityNodeInfo? {
    val root = InstrumentationRegistry.getInstrumentation().uiAutomation.rootInActiveWindow ?: return null
    fun find(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.text?.toString()?.equals(text, ignoreCase = true) == true ||
            node.contentDescription?.toString()?.equals(text, ignoreCase = true) == true) return node
        for (i in 0 until node.childCount) node.getChild(i)?.let { find(it)?.let { found -> return found } }
        return null
    }
    return find(root)
}

internal fun clickSystemText(vararg choices: String) {
    val until = SystemClock.uptimeMillis() + 10000
    while (SystemClock.uptimeMillis() < until) {
        for (text in choices) {
            var node = findSystemNode(text)
            while (node != null) {
                if (node.isClickable && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return
                node = node.parent
            }
        }
        SystemClock.sleep(100)
    }
    error("System control not found: ${choices.joinToString()}")
}

/** Host-only scenarios must be explicitly selected after saving/restoring device state. */
internal fun requireHostSetup(networkDisruption: Boolean = false) {
    val args = InstrumentationRegistry.getArguments()
    org.junit.Assume.assumeTrue("Use the PW04 verification host script", args.getString("verificationHostSetup") == "true")
    if (networkDisruption) org.junit.Assume.assumeTrue("Explicit network-disruption opt-in required",
        args.getString("allowNetworkDisruption") == "true")
}
