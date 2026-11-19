package ua.kpi.energyforecast.pw05

import android.Manifest
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import ua.kpi.energyforecast.pw05.domain.*
import ua.kpi.energyforecast.pw05.notifications.AlertCenter

/** Окремий запуск після pm revoke, з -e permissionDenied true.
 * Звичайна регресія пропускає сценарій; неправильна передумова явного запуску є помилкою. */
class PermissionDeniedDeviceTest {
    @Test fun deniedPermissionIsRecordedWithoutFalseDelivery() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue("POST_NOTIFICATIONS доступний з API 33", Build.VERSION.SDK_INT >= 33)
        assumeTrue("Потрібен окремий запуск -e permissionDenied true",
            InstrumentationRegistry.getArguments().getString("permissionDenied") == "true")
        val context = instrumentation.targetContext
        assertEquals("Перед запуском потрібно відкликати саме POST_NOTIFICATIONS",
            PackageManager.PERMISSION_DENIED, context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS))
        val center = AlertCenter.get(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        assertTrue(center.blocked(AlertCategory.FORECAST))
        val globalBefore = center.globallyEnabled()
        val categoryBefore = context.getSharedPreferences("alerts", 0).getBoolean(AlertCategory.FORECAST.name, true)
        var item: Alert? = null
        try {
            center.enable(null, true)
            center.enable(AlertCategory.FORECAST, true)
            item = center.create(AlertCategory.FORECAST, "Відмова дозволу", "Не повинно з'явитися у шторці")!!
            assertEquals(AlertStatus.BLOCKED, center.state.value.first { it.id == item.id }.status)
            assertFalse("Відхилений дозвіл не допускає системної нотифікації",
                manager.activeNotifications.any { it.id == item.id })
        } finally {
            item?.let { center.act(it.id, it.revision, AlertAction.CANCEL) }
            center.enable(AlertCategory.FORECAST, categoryBefore)
            center.enable(null, globalBefore)
        }
    }
}
