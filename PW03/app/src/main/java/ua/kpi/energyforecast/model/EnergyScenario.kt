package ua.kpi.energyforecast.model

/** Обмежений набір категорій із контракту навчального API. */
enum class ScenarioCategory {
    HOME,
    OFFICE,
    MIXED
}

/**
 * Перевірений сценарій для бізнес-логіки й UI.
 * На відміну від мережевих DTO, обов'язкові поля не nullable.
 */
data class EnergyScenario(
    val id: String,
    val name: String,
    val category: ScenarioCategory,
    val description: String,
    val devices: List<EnergyDevice>
)