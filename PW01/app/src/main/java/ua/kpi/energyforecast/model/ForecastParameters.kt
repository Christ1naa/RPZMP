package ua.kpi.energyforecast.model

data class ForecastParameters(
    val powerWatts: Double,
    val hoursPerDay: Double,
    val deviceCount: Int,
    val forecastDays: Int
)