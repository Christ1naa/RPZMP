package ua.kpi.energyforecast.pw05

import android.app.NotificationManager
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.Assert.*
import ua.kpi.energyforecast.pw05.notifications.AlertCenter
import ua.kpi.energyforecast.pw05.domain.*
import ua.kpi.energyforecast.pw05.data.SensorPreferences
import ua.kpi.energyforecast.pw05.ui.SensorSettings

/** Фази запускаються в різних процесах; між prepare/verify виконується справжній reboot. */
class ReliabilityDeviceTest {
    @Test fun persistedSettingsAndRebootDelivery() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val phase = InstrumentationRegistry.getArguments().getString("phase")
        org.junit.Assume.assumeTrue("Потрібен окремий двофазний запуск з reboot", phase in listOf("prepare", "verify"))
        val context = instrumentation.targetContext
        val center = AlertCenter.get(context)
        val marker = context.getSharedPreferences("reliability_test", 0)
        if (InstrumentationRegistry.getArguments().getString("phase") == "prepare") {
            assertFalse(center.blocked(AlertCategory.REMINDER))
            center.enable(null, true); center.enable(AlertCategory.REMINDER, true)
            val original = SensorPreferences(context).read()
            marker.edit().putString("selected", original.selected.joinToString(",") { it.name }).commit()
            SensorPreferences(context).write(original.copy(selected = setOf(SensorKind.ACCELEROMETER)))
            val item = center.create(AlertCategory.REMINDER, "PW05 reboot reliability", "Перевірка відновлення після reboot", 120_000)!!
            marker.edit().putInt("id", item.id).commit()
            assertEquals(AlertStatus.SCHEDULED, center.state.value.first { it.id == item.id }.status)
        } else {
            assertEquals(setOf(SensorKind.ACCELEROMETER), SensorPreferences(context).read().selected)
            val id = marker.getInt("id", -1)
            val deadline = System.currentTimeMillis() + 240_000
            while (System.currentTimeMillis() < deadline && center.state.value.first { it.id == id }.status == AlertStatus.SCHEDULED) Thread.sleep(500)
            val item = center.state.value.first { it.id == id }
            assertEquals(AlertStatus.SHOWN, item.status)
            assertTrue(context.getSystemService(NotificationManager::class.java).activeNotifications.any { it.id == id })
            center.act(id, item.revision, AlertAction.CANCEL)
            val selected = marker.getString("selected", "")!!.split(",").filter { it.isNotEmpty() }.map { SensorKind.valueOf(it) }.toSet()
            val current = SensorPreferences(context).read()
            SensorPreferences(context).write(current.copy(selected = selected))
        }
    }
}
