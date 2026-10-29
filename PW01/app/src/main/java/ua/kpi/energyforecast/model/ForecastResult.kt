package ua.kpi.energyforecast.model

/** Добова й періодна енергія у кВт·год без округлення; форматування належить UI. */
data class ForecastResult(
    val dailyConsumptionKwh: Double,
    val periodConsumptionKwh: Double,
    val forecastDays: Int
)