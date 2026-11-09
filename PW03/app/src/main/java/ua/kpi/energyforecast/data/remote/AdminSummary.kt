package ua.kpi.energyforecast.data.remote

data class AdminSummary(
    val scenarioCount: Int,
    val devicePositionCount: Int
)

/** Перевіряє відповідь до її відображення в інтерфейсі. */
fun AdminSummaryDto.toSummary(): AdminSummary {
    val scenarios = scenarioCount
    val devices = devicePositionCount

    if (
        scenarios == null ||
        devices == null ||
        scenarios < 0 ||
        devices < 0
    ) {
        throw InvalidApiDataException(
            "Некоректна адміністративна статистика."
        )
    }

    return AdminSummary(
        scenarioCount = scenarios,
        devicePositionCount = devices
    )
}