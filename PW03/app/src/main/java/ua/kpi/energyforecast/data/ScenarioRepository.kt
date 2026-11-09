package ua.kpi.energyforecast.data

import kotlinx.coroutines.flow.Flow
import ua.kpi.energyforecast.model.EnergyScenario
import ua.kpi.energyforecast.model.ForecastResult

/**
 * updatedAtMillis == null: каталог ще не завантажувався.
 * Порожній scenarios із часом оновлення: сервер повернув порожній каталог.
 */
data class ScenarioCatalog(
    val scenarios: List<EnergyScenario> = emptyList(),
    val updatedAtMillis: Long? = null
)

interface ScenarioRepository {

    fun observeCatalog(): Flow<ScenarioCatalog>

    /** Оновлює кеш або передає помилку виклику. */
    suspend fun refresh()

    suspend fun getCachedScenario(id: String): EnergyScenario?

    fun calculateForecast(
        scenario: EnergyScenario,
        forecastDays: Int
    ): ForecastResult
}