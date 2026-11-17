package ua.kpi.energyforecast.pw04

import android.view.WindowManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage

/** Лише вікно тесту: не змінює таймаут/блокування телефону. */
internal fun keepMapsTestScreenAwake() {
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
        ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).forEach {
            it.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
