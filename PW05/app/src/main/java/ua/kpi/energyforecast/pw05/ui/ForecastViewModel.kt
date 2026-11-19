package ua.kpi.energyforecast.pw05.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ua.kpi.energyforecast.pw05.data.DemoEnergyRepository
import ua.kpi.energyforecast.pw05.domain.EnergySeries

class ForecastViewModel(initialHours: Int = 24, private val persist: (Int) -> Unit = {}) : ViewModel() {
    private val history = DemoEnergyRepository().dailyHistory()
    private val mutableState = MutableStateFlow(createState(initialHours))
    val state = mutableState.asStateFlow()
    fun setHorizon(hours: Int) { mutableState.value = createState(hours); persist(hours) }
    private fun createState(hours: Int) = ForecastState(hours, requireNotNull(EnergySeries.forecast(history, hours)))
}
data class ForecastState(val hours: Int, val forecastKwh: Double)
