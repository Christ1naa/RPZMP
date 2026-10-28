package ua.kpi.energyforecast.model

data class EnergyDevice(
    val id: String,
    val name: String,
    val powerWatts: Double,
    val hoursPerDay: Double,
    val deviceCount: Int
)