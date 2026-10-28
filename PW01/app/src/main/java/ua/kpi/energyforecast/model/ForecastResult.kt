package ua.kpi.energyforecast.model

/**
 * Результат розрахунку споживання.
 * Одиниця вимірювання енергії — кВт·год.
 */
data class ForecastResult(
    val dailyConsumptionKwh: Double,
    val periodConsumptionKwh: Double,
    val forecastDays: Int
)