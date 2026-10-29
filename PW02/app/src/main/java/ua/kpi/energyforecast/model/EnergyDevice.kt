package ua.kpi.energyforecast.model

/** Група однакових приладів: стабільний id, потужність одного у Вт, години на добу та кількість. */
data class EnergyDevice(
    val id: String,
    val name: String,
    val powerWatts: Double,
    val hoursPerDay: Double,
    val deviceCount: Int
)