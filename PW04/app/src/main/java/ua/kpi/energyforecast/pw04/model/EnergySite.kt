package ua.kpi.energyforecast.pw04.model

import java.time.Instant

enum class SiteType(val label: String) {
    HOME("Житло"), OFFICE("Офіс"), INDUSTRY("Виробництво")
}
enum class SiteStatus(val label: String) { ACTIVE("Активний"), MAINTENANCE("Обслуговування") }

data class Coordinates(val latitude: Double, val longitude: Double) {
    init {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
    }
}

data class EnergySite(
    val id: String,
    val name: String,
    val type: SiteType,
    val description: String,
    val position: Coordinates,
    val ratedPowerKw: Double,
    val hoursPerDay: Double,
    val status: SiteStatus,
    val updatedAt: Instant
) {
    init {
        require(id.isNotBlank() && name.isNotBlank())
        require(ratedPowerKw.isFinite() && ratedPowerKw > 0)
        require(hoursPerDay.isFinite() && hoursPerDay in 0.0..24.0)
    }
}

data class SiteReading(
    val site: EnergySite,
    val measuredPowerKw: Double,
    val forecastDailyKwh: Double,
    val sampledAt: Instant
)
