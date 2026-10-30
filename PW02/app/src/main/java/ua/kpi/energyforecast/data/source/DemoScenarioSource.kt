package ua.kpi.energyforecast.data.source

import ua.kpi.energyforecast.model.EnergyDevice

/** Приклад параметрів, який користувач може імпортувати у власний профіль. */
data class DemoScenario(
    val id: String,
    val name: String,
    val forecastDays: Int,
    val devices: List<EnergyDevice>
)

/** Джерело прикладів, незалежне від способу зберігання. */
fun interface DemoScenarioSource {
    suspend fun load(): List<DemoScenario>
}