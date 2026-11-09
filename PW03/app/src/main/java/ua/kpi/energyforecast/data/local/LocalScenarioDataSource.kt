package ua.kpi.energyforecast.data.local

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ua.kpi.energyforecast.data.ScenarioCatalog
import ua.kpi.energyforecast.model.EnergyDevice
import ua.kpi.energyforecast.model.EnergyScenario
import ua.kpi.energyforecast.model.ScenarioCategory

class LocalScenarioDataSource(
    private val database: EnergyDatabase
) {

    private val dao = database.scenarioCacheDao()

    fun observeCatalog(): Flow<ScenarioCatalog> {
        return dao.observeCatalog().map { saved ->
            if (saved == null) {
                ScenarioCatalog()
            } else {
                ScenarioCatalog(
                    scenarios = saved.scenarios
                        .sortedBy { it.scenario.id }
                        .map { it.toModel() },
                    updatedAtMillis = saved.metadata.updatedAtMillis
                )
            }
        }
    }

    suspend fun getScenario(id: String): EnergyScenario? {
        return observeCatalog().first().scenarios.firstOrNull {
            it.id == id
        }
    }

    /**
     * Приймає перевірені моделі.
     * Каталог і час оновлення замінюються однією транзакцією.
     */
    suspend fun replaceCatalog(
        scenarios: List<EnergyScenario>,
        updatedAtMillis: Long
    ) {
        require(updatedAtMillis >= 0)

        val scenarioRows = scenarios.map { scenario ->
            CachedScenarioEntity(
                id = scenario.id,
                name = scenario.name,
                category = scenario.category.name,
                description = scenario.description
            )
        }

        val deviceRows = scenarios.flatMap { scenario ->
            scenario.devices.mapIndexed { position, device ->
                CachedScenarioDeviceEntity(
                    scenarioId = scenario.id,
                    position = position,
                    deviceId = device.id,
                    name = device.name,
                    powerWatts = device.powerWatts,
                    hoursPerDay = device.hoursPerDay,
                    deviceCount = device.deviceCount
                )
            }
        }

        database.withTransaction {
            // CASCADE видаляє лише попередній каталог і його прилади.
            dao.deleteCatalog()

            dao.insertMetadata(
                ScenarioCacheMetaEntity(
                    updatedAtMillis = updatedAtMillis
                )
            )

            dao.insertScenarios(scenarioRows)
            dao.insertDevices(deviceRows)
        }
    }

    private fun CachedScenarioWithDevices.toModel(): EnergyScenario {
        return EnergyScenario(
            id = scenario.id,
            name = scenario.name,
            category = ScenarioCategory.valueOf(scenario.category),
            description = scenario.description,
            devices = devices.sortedBy { it.position }.map { device ->
                EnergyDevice(
                    id = device.deviceId,
                    name = device.name,
                    powerWatts = device.powerWatts,
                    hoursPerDay = device.hoursPerDay,
                    deviceCount = device.deviceCount
                )
            }
        )
    }
}