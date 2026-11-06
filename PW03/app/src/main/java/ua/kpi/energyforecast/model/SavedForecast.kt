package ua.kpi.energyforecast.model

/**
 * Збережений результат і копії параметрів приладів.
 * createdAt містить час Unix у мілісекундах; форматування виконує UI.
 */
data class SavedForecast(
    val id: String,
    val title: String,
    val createdAt: Long,
    val result: ForecastResult,
    val devices: List<EnergyDevice>
)