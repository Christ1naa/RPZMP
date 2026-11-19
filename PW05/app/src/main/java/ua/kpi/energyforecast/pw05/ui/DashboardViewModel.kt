package ua.kpi.energyforecast.pw05.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ua.kpi.energyforecast.pw05.data.DemoEnergyRepository
import ua.kpi.energyforecast.pw05.domain.*

data class DashboardState(val period: ChartPeriod = ChartPeriod.DAY, val zoom: Int = 1, val offset: Int = 0, val generation: Int = 0, val scale: Float = 1f, val alternate: Boolean = false, val large: Boolean = false)
class DashboardViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("dashboard", 0)
    private val mutable = MutableStateFlow(DashboardState(
        period = ChartPeriod.entries.getOrElse(prefs.getInt("period", 0)) { ChartPeriod.DAY },
        zoom = prefs.getInt("zoom", 1).coerceIn(1, 4), offset = prefs.getInt("offset", 0).coerceAtLeast(0),
        scale = prefs.getFloat("scale", 1f).takeIf { it.isFinite() }?.coerceIn(0.5f, 2f) ?: 1f,
        alternate = prefs.getBoolean("alternate", false), large = prefs.getBoolean("large", false)))
    val state = mutable.asStateFlow()
    fun update(value: DashboardState) {
        require(value.zoom in 1..4 && value.scale.isFinite() && value.scale in 0.5f..2f)
        mutable.value = value
        prefs.edit().putInt("period", value.period.ordinal).putInt("zoom", value.zoom).putInt("offset", value.offset)
            .putFloat("scale", value.scale).putBoolean("alternate", value.alternate).putBoolean("large", value.large).apply()
    }
    fun points(s: DashboardState): List<ChartPoint> {
        val day = DemoEnergyRepository().dailyHistory()
        // 30 діб навчальної історії. Оновлення змінює сценарій, а не видає його за виміри.
        val all = (0 until 720).map { h -> HourlyEnergy(h, day[h % 24].kwh * (1 + (h / 24 % 5 - 4) * 0.03 + s.generation % 4 * 0.02)) }
        return ChartData.aggregate(all, s.period)
    }
}

