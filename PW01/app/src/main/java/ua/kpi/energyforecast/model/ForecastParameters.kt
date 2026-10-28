package ua.kpi.energyforecast.model

/**
 * Параметри прогнозування для групи однакових приладів.
 */
data class ForecastParameters(
    val powerWatts: Double,
    val hoursPerDay: Double,
    val deviceCount: Int,
    val forecastDays: Int
)