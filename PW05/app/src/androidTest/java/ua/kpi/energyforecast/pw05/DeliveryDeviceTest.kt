package ua.kpi.energyforecast.pw05

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.Assert.*
import ua.kpi.energyforecast.pw05.domain.*
import ua.kpi.energyforecast.pw05.notifications.AlertCenter
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay

class DeliveryDeviceTest {
    @Test fun alarmDeliversAfterRequestedTime() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val center = AlertCenter.get(context)
        center.enable(null, true)
        center.enable(AlertCategory.REMINDER, true)
        val item = center.create(AlertCategory.REMINDER, "Таймер перевірки PW05", "Справжня доставка неточного AlarmManager", 2000)!!
        assertEquals(AlertStatus.SCHEDULED, center.state.value.find { it.id == item.id }!!.status)
        val deadline = System.currentTimeMillis() + 90000
        while (System.currentTimeMillis() < deadline && center.state.value.find { it.id == item.id }!!.status == AlertStatus.SCHEDULED) delay(500)
        val updated = center.state.value.find { it.id == item.id }!!
        assertEquals("Система не доставила неточний alarm протягом 90 с", AlertStatus.SHOWN, updated.status)
        assertTrue(System.currentTimeMillis() >= item.due)
        center.act(item.id, updated.revision, AlertAction.CANCEL)
    }
}

