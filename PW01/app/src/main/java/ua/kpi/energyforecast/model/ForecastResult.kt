package ua.kpi.energyforecast.model

data class ForecastResult(
    val dailyConsumptionKwh: Double,
    val periodConsumptionKwh: Double,
    val forecastDays: Int
)