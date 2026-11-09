package ua.kpi.energyforecast.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ua.kpi.energyforecast.data.local.LocalScenarioDataSource
import ua.kpi.energyforecast.data.remote.ScenarioRemoteSource
import ua.kpi.energyforecast.logic.EnergyCalculator
import ua.kpi.energyforecast.model.EnergyScenario
import ua.kpi.energyforecast.model.ForecastResult

/**
 * Room є джерелом даних для спостереження.
 * Мережа оновлює Room лише після успішного отримання й перевірки відповіді.
 */
class RoomScenarioRepository(
    private val local: LocalScenarioDataSource,
    private val remote: ScenarioRemoteSource,
    private val clock: () -> Long = { System.currentTimeMillis() }
) : ScenarioRepository {

    private val refreshMutex = Mutex()

    override fun observeCatalog(): Flow<ScenarioCatalog> {
        return local.observeCatalog()
    }

    override suspend fun refresh() {
        // Одночасні оновлення одного Repository виконуються послідовно.
        refreshMutex.withLock {
            val scenarios = remote.loadScenarios()

            local.replaceCatalog(
                scenarios = scenarios,
                updatedAtMillis = clock()
            )
        }
    }

    override suspend fun getCachedScenario(id: String): EnergyScenario? {
        require(id.isNotBlank()) {
            "ID сценарію не може бути порожнім"
        }

        return local.getScenario(id)
    }

    override fun calculateForecast(
        scenario: EnergyScenario,
        forecastDays: Int
    ): ForecastResult {
        return EnergyCalculator.calculateTotal(
            devices = scenario.devices,
            forecastDays = forecastDays
        )
    }
}