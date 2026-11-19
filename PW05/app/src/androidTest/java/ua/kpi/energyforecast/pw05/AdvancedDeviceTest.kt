package ua.kpi.energyforecast.pw05

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.*
import org.junit.Assert.*
import ua.kpi.energyforecast.pw05.domain.*
import ua.kpi.energyforecast.pw05.notifications.AlertCenter
import ua.kpi.energyforecast.pw05.data.SensorPreferences
import ua.kpi.energyforecast.pw05.ui.SensorSettings
import java.io.File

class AdvancedDeviceTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private fun screenshot(name: String) {
        if (!name.startsWith("07-")) ui.waitForIdle()

        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        try {
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                val values = android.content.ContentValues().apply {
                    val prefix = InstrumentationRegistry.getArguments().getString("screenshotPrefix") ?: ""
                    put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "$prefix$name.png")
                    put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PW05")
                }
                val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
                context.contentResolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        } finally { bitmap.recycle() }
    }
    @Test fun chartsComponentsAndRotation() {
        ui.onNodeWithText("Графіки споживання").performScrollTo().assertIsDisplayed()
        ui.onAllNodesWithText("Наступне значення / підказка")[0].performScrollTo()
        screenshot("01-charts")
        ui.onAllNodesWithText("Наступне значення / підказка")[0].performScrollTo().performClick()
        ui.onNodeWithText("Підказка:", substring = true).performScrollTo().assertIsDisplayed()
        screenshot("02-chart-tooltip")
        ui.onNodeWithText("Тиждень", substring = false).performScrollTo().performClick()
        ui.onAllNodesWithText("Наступне значення / підказка")[0].performScrollTo()
        screenshot("03-week")
        ui.onNodeWithText("Власні компоненти").performScrollTo().performClick()
        ui.onNodeWithText("Змінити кольори").performScrollTo().performClick()
        if (ui.onAllNodesWithText("Розмір: звичайний").fetchSemanticsNodes().isNotEmpty())
            ui.onNodeWithText("Розмір: звичайний").performScrollTo().performClick()
        ui.onNodeWithText("Легенда:", substring = true).performScrollTo()
        screenshot("04-components")
        ui.activityRule.scenario.recreate()
        ui.onNodeWithText("Власні сенсорні компоненти").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("Розмір: великий").performScrollTo().assertIsDisplayed()
        screenshot("05-recreated")
    }
    @Test fun notificationActionsUseSystemPendingIntentsAndPersistState() {
        instrumentation.uiAutomation.executeShellCommand("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS").close()
        val center = AlertCenter.get(context)
        center.enable(null, true)
        AlertCategory.entries.forEach { center.enable(it, true) }
        val first = center.create(AlertCategory.FORECAST, "Тест прогнозу", "Демонстраційна подія")!!
        val second = center.create(AlertCategory.REMINDER, "Тест нагадування", "Друга незалежна подія")!!
        val manager = context.getSystemService(NotificationManager::class.java)
        ui.waitUntil(5000) { manager.activeNotifications.count { it.id in listOf(first.id, second.id) } == 2 }
        Thread.sleep(2000)
        instrumentation.uiAutomation.executeShellCommand("cmd statusbar expand-notifications").use { java.io.FileInputStream(it.fileDescriptor).readBytes() }
        Thread.sleep(2000)
        screenshot("07-system-notifications")
        instrumentation.uiAutomation.executeShellCommand("cmd statusbar collapse").close()
        manager.activeNotifications.find { it.id == first.id }!!.notification.actions[1].actionIntent.send()
        ui.waitUntil(5000) { center.state.value.find { it.id == first.id }!!.revision == 1 }
        val snoozed = center.state.value.find { it.id == first.id }!!
        assertEquals(AlertStatus.SCHEDULED, snoozed.status)
        assertTrue(snoozed.due > System.currentTimeMillis() + 290000)
        manager.activeNotifications.find { it.id == second.id }!!.notification.actions[0].actionIntent.send()
        ui.waitUntil(5000) { center.state.value.find { it.id == second.id }!!.status == AlertStatus.CONFIRMED }
        val json = context.getSharedPreferences("alerts", Context.MODE_PRIVATE).getString("journal", "")!!
        assertTrue(json.contains("SCHEDULED") && json.contains("CONFIRMED"))
        context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("alert_id", first.id))
        ui.onNodeWithText("Локальні сповіщення").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("Деталі №${first.id}").performScrollTo()
        screenshot("06-notification-journal")
        center.act(first.id, snoozed.revision, AlertAction.CANCEL)
        assertFalse(manager.activeNotifications.any { it.id == first.id || it.id == second.id })
    }
    @Test fun sensorSettingsSurviveRepositoryRecreation() {
        val storage = SensorPreferences(context)
        val original = storage.read()
        val options = SensorSettings(setOf(SensorKind.LIGHT), SensorKind.entries.associateWith { 17f })
        try {
            storage.write(options)
            assertEquals(options, SensorPreferences(context).read())
        } finally { storage.write(original) }
    }

    @Test fun categoryDisableCancelsAllPendingAndBlocksNewEvents() {
        val center = AlertCenter.get(context)
        center.enable(null, true)
        center.enable(AlertCategory.REMINDER, true)
        val one = center.create(AlertCategory.REMINDER, "Один", "Тест", 60000)!!
        val two = center.create(AlertCategory.REMINDER, "Два", "Тест", 120000)!!
        assertNotEquals(one.id, two.id)
        center.enable(AlertCategory.REMINDER, false)
        assertEquals(AlertStatus.CANCELLED, center.state.value.find { it.id == one.id }!!.status)
        assertEquals(AlertStatus.CANCELLED, center.state.value.find { it.id == two.id }!!.status)
        assertNull(center.create(AlertCategory.REMINDER, "Три", "Тест"))
        center.enable(AlertCategory.REMINDER, true)
    }

    @Test fun largeFontLandscapeAndSensorSelectionRemainReachable() {
        try {
            instrumentation.uiAutomation.executeShellCommand("settings put system font_scale 1.5").use {
                java.io.FileInputStream(it.fileDescriptor).readBytes()
            }
            ui.activity.runOnUiThread { ui.activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            ui.waitUntil(10000) { ui.activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE }
            ui.onNodeWithText("Сенсори", substring = false).performScrollTo().performClick()
            ui.onNodeWithContentDescription("Вибір: Освітленість").performScrollTo().assertIsDisplayed()
            val node = ui.onNodeWithContentDescription("Вибір: Освітленість")
            node.performClick()
            screenshot("08-landscape-large-font")
            node.performClick()
            ui.onNodeWithText("Сортування:", substring = true).performScrollTo().assertIsDisplayed()
            screenshot("09-sensor-query-large-font")
        } finally {
            instrumentation.uiAutomation.executeShellCommand("settings put system font_scale 1.0").use {
                java.io.FileInputStream(it.fileDescriptor).readBytes()
            }
            ui.activity.runOnUiThread { ui.activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
        }
    }

    @Test fun chartModesZoomAndCategoryTooltip() {
        ui.onNodeWithText("Вигляд: Комбінований").performScrollTo().performClick()
        ui.onNodeWithText("Вигляд: Лінія").assertExists()
        ui.onAllNodesWithText("Наступне значення / підказка")[0].performScrollTo()
        screenshot("10-line")
        ui.onNodeWithText("Вигляд: Лінія").performScrollTo().performClick()
        ui.onAllNodesWithText("Наступне значення / підказка")[0].performScrollTo()
        screenshot("11-bars")
        ui.onAllNodes(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsActions.SetProgress))[0].performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(2f) }
        ui.onNodeWithText("Масштаб: 2×").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("Вигляд: Стовпчики").performScrollTo().performClick()
        ui.onAllNodesWithText("Наступне значення / підказка")[0].performScrollTo()
        screenshot("12-combined-zoom2")
        ui.onAllNodesWithText("Наступне значення / підказка")[1].performScrollTo().performClick()
        ui.onNodeWithText("Підказка: Житлова", substring = true).performScrollTo().assertIsDisplayed()
        screenshot("13-zone-tooltip")
    }
    /** Кадри лише фактичного UI фізичного пристрою, без підставних сенсорних значень. */
    @Test fun physicalSensorsAndQueryScreenshots() {
        Assume.assumeTrue("Окремий сценарій отримання доказів", InstrumentationRegistry.getArguments().getString("screenshotPrefix") != null)
        val original = SensorPreferences(context).read()
        try {
            ui.onNodeWithText("Прогноз споживання електроенергії").assertExists()
            screenshot("15-overview")
            ui.onNodeWithText("Сенсори", substring = false).performScrollTo().performClick()
            ui.onNodeWithText("Моніторинг сенсорів").performScrollTo()
            Thread.sleep(2000)
            screenshot("16-sensors-first")
            Thread.sleep(15000)
            screenshot("17-sensors-second")
            ui.onNodeWithContentDescription("Вибір: Гіроскоп").performScrollTo()
            screenshot("18-gyroscope")
            ui.onNodeWithContentDescription("Вибір: Освітленість").performScrollTo()
            screenshot("19-light")
            ui.onNodeWithText("Тип: Усі").performScrollTo().performClick()
            ui.onNodeWithText("Мінімальний модуль").performScrollTo().performTextReplacement("0")
            ui.onNodeWithText("Максимальний модуль").performScrollTo().performTextReplacement("20")
            ui.onNodeWithText("Сортування:", substring=true).performScrollTo().performClick()
            ui.onNodeWithText("Збігів:", substring=true).performScrollTo()
            screenshot("20-filter-sort")
        } finally { SensorPreferences(context).write(original) }
    }    @Test fun physicalReportSupplement() {
        Assume.assumeTrue("Окремий сценарій отримання доказів", InstrumentationRegistry.getArguments().getString("screenshotPrefix") != null)
        ui.onNodeWithText("Сенсори", substring=false).performScrollTo().performClick()
        ui.onNodeWithText("Поріг (рад/с)").performScrollTo()
        screenshot("21-gyro-card")
        ui.onNodeWithText("Поріг (лк)").performScrollTo()
        screenshot("22-light-card")
        ui.onNodeWithText("Тип: Усі").performScrollTo().performClick()
        ui.onNodeWithText("Мінімальний модуль").performScrollTo().performTextReplacement("0")
        ui.onNodeWithText("Максимальний модуль").performScrollTo().performTextReplacement("20")
        instrumentation.uiAutomation.executeShellCommand("input keyevent 4").use { java.io.FileInputStream(it.fileDescriptor).readBytes() }
        ui.onNodeWithText("Сортування:", substring=true).performScrollTo().performClick()
        ui.onNodeWithText("Збігів:", substring=true).performScrollTo()
        screenshot("23-query-clear")
        ui.onNodeWithText("Сповіщення", substring=false).performScrollTo().performClick()
        ui.onNodeWithText("Канал «Нагадування»: звук і пріоритет").performScrollTo()
        screenshot("24-channels")
        val center = AlertCenter.get(context)
        val item = center.create(AlertCategory.REMINDER, "PW05: деталі події", "Контрольний запис для звіту; локальне нагадування", 600000)!!
        try {
            ui.onNodeWithText("Деталі №${item.id}").performScrollTo().performClick()
            ui.onNodeWithText("Контрольний запис для звіту; локальне нагадування", substring=true).performScrollTo()
            screenshot("25-event-details")
        } finally { center.act(item.id, item.revision, AlertAction.CANCEL) }
    }    @Test fun physicalThresholdEvidence() {
        Assume.assumeTrue("Окремий сценарій отримання доказів", InstrumentationRegistry.getArguments().getString("screenshotPrefix") != null)
        val original = SensorPreferences(context).read()
        try {
            ui.onNodeWithText("Сенсори", substring=false).performScrollTo().performClick()
            ui.onNodeWithText("Поріг (м/с²)").performScrollTo().performTextReplacement("1")
            instrumentation.uiAutomation.executeShellCommand("input keyevent 4").use { java.io.FileInputStream(it.fileDescriptor).readBytes() }
            ui.onAllNodesWithText("Поточний поріг:", substring=true)[0].performScrollTo()
            screenshot("29-threshold-exceeded")
        } finally { SensorPreferences(context).write(original) }
    }
    @Test fun physicalPermissionDeniedEvidence() {
        Assume.assumeTrue("Окремий сценарій отримання доказів", InstrumentationRegistry.getArguments().getString("screenshotPrefix") != null)
        ui.onNodeWithText("Сповіщення", substring=false).performScrollTo().performClick()
        ui.onNodeWithText("Прогноз: системний дозвіл або канал заблокований").performScrollTo().assertIsDisplayed()
        screenshot("30-permission-denied")
    }    @Test fun physicalSnoozeAndConfirmEvidence() {
        Assume.assumeTrue("Окремий п'ятихвилинний сценарій доказів", InstrumentationRegistry.getArguments().getString("screenshotPrefix") != null)
        val center = AlertCenter.get(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        val one = center.create(AlertCategory.REMINDER, "PW05: відкладання на 5 хв", "Фактична повторна доставка на Galaxy A54")!!
        val two = center.create(AlertCategory.FORECAST, "PW05: підтвердження", "Незалежна подія для перевірки дії")!!
        try {
            ui.waitUntil(5000) { manager.activeNotifications.any { it.id == one.id } && manager.activeNotifications.any { it.id == two.id } }
            manager.activeNotifications.first { it.id == one.id }.notification.actions[1].actionIntent.send()
            manager.activeNotifications.first { it.id == two.id }.notification.actions[0].actionIntent.send()
            ui.waitUntil(5000) { center.state.value.first { it.id == one.id }.revision == 1 && center.state.value.first { it.id == two.id }.status == AlertStatus.CONFIRMED }
            ui.onNodeWithText("Сповіщення", substring=false).performScrollTo().performClick()
            ui.onNodeWithText("Деталі №${two.id}").performScrollTo()
            screenshot("31-confirmed")
            ui.onNodeWithText("Деталі №${one.id}").performScrollTo()
            screenshot("32-snoozed")
            val scheduled = center.state.value.first { it.id == one.id }
            val deadline = scheduled.due + 300000
            while (System.currentTimeMillis() < deadline && center.state.value.first { it.id == one.id }.status == AlertStatus.SCHEDULED) Thread.sleep(500)
            assertEquals(AlertStatus.SHOWN, center.state.value.first { it.id == one.id }.status)
            assertTrue(manager.activeNotifications.any { it.id == one.id })
            ui.onNodeWithText("Деталі №${one.id}").performScrollTo()
            screenshot("33-snooze-delivered")
        } finally {
            center.state.value.firstOrNull { it.id == one.id }?.let { center.act(it.id, it.revision, AlertAction.CANCEL) }
            center.state.value.firstOrNull { it.id == two.id }?.let { center.act(it.id, it.revision, AlertAction.CANCEL) }
        }
    }}

