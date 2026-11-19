package ua.kpi.energyforecast.pw05

import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.Assert.*
import ua.kpi.energyforecast.pw05.domain.*
import ua.kpi.energyforecast.pw05.notifications.AlertCenter

/** Окремий запуск після основної регресії. Після тесту канал Прогноз залишається заблокованим;
 * чисте перевстановлення тестового пакета повертає початкові канали. */
class NotificationPolicyDeviceTest {
    @Test fun blockedChannelIsReportedWithoutFalseDelivery() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val center = AlertCenter.get(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        center.enable(null, true); center.enable(AlertCategory.FORECAST, true)
        // Android дозволяє застосунку знизити важливість каналу, якщо користувач не змінював її.
        manager.createNotificationChannel(NotificationChannel(AlertCategory.FORECAST.name, "Прогноз", NotificationManager.IMPORTANCE_NONE))
        assertEquals(NotificationManager.IMPORTANCE_NONE, manager.getNotificationChannel(AlertCategory.FORECAST.name).importance)
        assertTrue(center.blocked(AlertCategory.FORECAST))
        val item = center.create(AlertCategory.FORECAST, "Заблокований канал", "Тест")!!
        assertEquals(AlertStatus.BLOCKED, center.state.value.find { it.id == item.id }!!.status)
        center.act(item.id, item.revision, AlertAction.CANCEL)
    }
    @Test fun sensorCooldownSuppressesRepeatedTestFixtures() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val center = AlertCenter.get(context)
        center.enable(null, true); center.enable(AlertCategory.SENSOR, true)
        val prefs = context.getSharedPreferences("alerts", 0)
        val previous = prefs.getLong("last_LIGHT", 0)
        prefs.edit().remove("last_LIGHT").commit()
        val before = center.state.value.map { it.id }.toSet()
        try {
            // Контрольовані вхідні fixtures, не виміри фізичного телефона.
            center.sensor(SensorKind.LIGHT, SensorReading(SensorKind.LIGHT, 1, listOf(600f), 600f), 500f)
            center.sensor(SensorKind.LIGHT, SensorReading(SensorKind.LIGHT, 2, listOf(700f), 700f), 500f)
            val added = center.state.value.filter { it.id !in before && it.category == AlertCategory.SENSOR }
            assertEquals(1, added.size)
            assertTrue(prefs.getLong("last_LIGHT", 0) > 0)
            added.forEach { center.act(it.id, it.revision, AlertAction.CANCEL) }
        } finally { prefs.edit().putLong("last_LIGHT", previous).commit() }
    }
}

