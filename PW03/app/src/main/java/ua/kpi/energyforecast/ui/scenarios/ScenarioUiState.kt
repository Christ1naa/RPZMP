package ua.kpi.energyforecast.ui.scenarios

import ua.kpi.energyforecast.data.ScenarioCatalog
import ua.kpi.energyforecast.logic.DeviceValidator
import ua.kpi.energyforecast.model.EnergyScenario
import ua.kpi.energyforecast.model.ForecastResult
import ua.kpi.energyforecast.model.ScenarioCategory

data class ScenarioUiState(
    val catalog: ScenarioCatalog = ScenarioCatalog(),
    val isLoadingCache: Boolean = true,
    val isRefreshing: Boolean = false,
    val cacheError: String? = null,
    val refreshError: String? = null,
    val query: String = "",
    val category: ScenarioCategory? = null,
    val sort: ScenarioSort = ScenarioSort.NAME_ASC,
    val selectedScenarioId: String? = null,
    val daysText: String = "30",
    val importingScenarioId: String? = null,
    val message: String? = null,
    val calculatedItems: List<ScenarioListItem> = emptyList(),
    val selectedForecast: ForecastResult? = null
) {

    val visibleItems: List<ScenarioListItem>
        get() = selectScenarios(
            scenarios = calculatedItems,
            query = query,
            category = category,
            sort = sort
        )

    /**
     * Деталі залежать від повного каталогу.
     * Зміна пошукового фільтра не змінює відкритий сценарій.
     */
    val selectedScenario: EnergyScenario?
        get() = catalog.scenarios.firstOrNull {
            it.id == selectedScenarioId
        }

    val forecastDays: Int?
        get() = DeviceValidator.parseForecastDays(daysText)

    val hasCachedCatalog: Boolean
        get() = catalog.updatedAtMillis != null

    val isImporting: Boolean
        get() = importingScenarioId != null
}
