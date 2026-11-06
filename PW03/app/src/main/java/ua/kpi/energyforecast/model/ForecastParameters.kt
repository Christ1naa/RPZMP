package ua.kpi.energyforecast.model

/** Потужність одного приладу у Вт, години на добу, кількість однакових приладів і дні прогнозу. */
data class ForecastParameters(
    val powerWatts: Double,
    val hoursPerDay: Double,
    val deviceCount: Int,
    val forecastDays: Int
)