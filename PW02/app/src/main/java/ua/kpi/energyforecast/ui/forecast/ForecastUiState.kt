package ua.kpi.energyforecast.ui.forecast

import ua.kpi.energyforecast.logic.DeviceValidator
import ua.kpi.energyforecast.model.EnergyDevice
import ua.kpi.energyforecast.model.ForecastResult

/**
 * Узгоджений стан списку й прогнозу.
 * Результат очищується на час перерахунку, щоб UI не показував застаріле значення.
 */
data class ForecastUiState(
    val devices: List<EnergyDevice> = emptyList(),
    val daysText: String = "30",
    val result: ForecastResult? = null,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val forecastDays: Int?
        get() = DeviceValidator.parseForecastDays(daysText)

    val totalDeviceCount: Int
        get() = devices.sumOf { it.deviceCount }

    val canAddDevice: Boolean
        get() = devices.size < MAX_DEVICE_POSITIONS

    companion object {
        const val MAX_DEVICE_POSITIONS = 50
    }
}