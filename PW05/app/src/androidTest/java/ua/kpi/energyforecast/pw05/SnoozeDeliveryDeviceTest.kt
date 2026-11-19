package ua.kpi.energyforecast.pw05

import android.app.NotificationManager
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.Assert.*
import ua.kpi.energyforecast.pw05.domain.*
import ua.kpi.energyforecast.pw05.notifications.AlertCenter

/** Повний п'ятихвилинний цикл. Запускати окремо на чистому тестовому встановленні з дозволом. */
class SnoozeDeliveryDeviceTest {
    @Test fun snoozedSystemActionActuallyDeliversAgainAfterFiveMinutes() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val center = AlertCenter.get(context)
        center.enable(null, true); center.enable(AlertCategory.REMINDER, true)
        assertFalse(center.blocked(AlertCategory.REMINDER))
        val manager = context.getSystemService(NotificationManager::class.java)
        if (InstrumentationRegistry.getArguments().getString("verifyPersistedResult") == "true") {
            // Окрема перевірка збереженого результату попереднього циклу у свіжому процесі.
            // Package update може прибрати нотифікацію зі шторки; це не повторна перевірка показу.
            val completed = center.state.value.last { it.title == "PW05 Відкладання" && it.revision == 1 }
            assertEquals(AlertStatus.SHOWN, completed.status)
            assertTrue(System.currentTimeMillis() >= completed.due)
            center.act(completed.id, completed.revision, AlertAction.CANCEL)
            return@runBlocking
        }
        val item = center.create(AlertCategory.REMINDER, "PW05 Відкладання", "Повторний показ через п'ять хвилин")!!
        val first = manager.activeNotifications.first { it.id == item.id }
        first.notification.actions[1].actionIntent.send()
        val actionDeadline = System.currentTimeMillis() + 5000
        while (System.currentTimeMillis() < actionDeadline && center.state.value.first { it.id == item.id }.revision == 0) delay(50)
        val scheduled = center.state.value.first { it.id == item.id }
        assertEquals(AlertStatus.SCHEDULED, scheduled.status)
        assertEquals(1, scheduled.revision)
        // NotificationManager скасовує показ асинхронно відносно оновлення журналу.
        val removalDeadline = System.currentTimeMillis() + 5000
        while (System.currentTimeMillis() < removalDeadline && manager.activeNotifications.any { it.id == item.id }) delay(50)
        assertFalse(manager.activeNotifications.any { it.id == item.id })
        val deadline = scheduled.due + 300000
        while (System.currentTimeMillis() < deadline && center.state.value.first { it.id == item.id }.status == AlertStatus.SCHEDULED) delay(500)
        val delivered = center.state.value.first { it.id == item.id }
        assertEquals(AlertStatus.SHOWN, delivered.status)
        assertEquals(1, delivered.revision)
        assertTrue(System.currentTimeMillis() >= scheduled.due)
        assertTrue(manager.activeNotifications.any { it.id == item.id })
        center.act(item.id, delivered.revision, AlertAction.CANCEL)
    }
}

