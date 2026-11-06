package ua.kpi.energyforecast.data

import androidx.room.withTransaction
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ua.kpi.energyforecast.data.local.EnergyDatabase
import ua.kpi.energyforecast.data.local.toEntity
import ua.kpi.energyforecast.data.local.toModel
import ua.kpi.energyforecast.data.source.CachedDemoScenarioSource
import ua.kpi.energyforecast.data.source.DemoScenario
import ua.kpi.energyforecast.logic.DeviceValidator
import ua.kpi.energyforecast.logic.EnergyCalculator
import ua.kpi.energyforecast.model.EnergyDevice
import ua.kpi.energyforecast.model.ForecastResult

/**
 * Працює з профілем у Room і кешованими прикладами з assets.
 * Перевіряє записи, керує імпортом і делегує формули EnergyCalculator.
 */
class RoomEnergyRepository(
    private val database: EnergyDatabase,
    private val demoSource: CachedDemoScenarioSource
) : EnergyRepository {

    private val dao = database.energyDao()

    override fun observeProfile(
        ownerKey: String
    ): Flow<SavedEnergyProfile> {
        requireOwner(ownerKey)

        return dao.observeProfile(ownerKey).map { saved ->
            SavedEnergyProfile(
                devices = saved?.devices
                    ?.map { it.toModel() }
                    ?.sortedBy { it.id }
                    .orEmpty(),
                forecastDays = saved?.profile?.forecastDays ?: 30,
                dailyConsumptionKwh = saved?.dailyConsumptionKwh ?: 0.0
            )
        }
    }

    override suspend fun saveDevice(
        ownerKey: String,
        device: EnergyDevice
    ) {
        requireOwner(ownerKey)
        val normalized = normalizeDevice(device)

        // Перевірка ліміту та запис виконуються атомарно.
        database.withTransaction {
            val current = dao.observeProfile(ownerKey)
                .first()
                ?.devices
                .orEmpty()

            val exists = current.any { it.id == normalized.id }

            require(exists || current.size < MAX_DEVICE_POSITIONS) {
                "Досягнуто межі: 50 позицій."
            }

            dao.saveDevice(normalized.toEntity(ownerKey))
        }
    }

    override suspend fun deleteDevice(
        ownerKey: String,
        deviceId: String
    ) {
        requireOwner(ownerKey)
        require(deviceId.isNotBlank()) {
            "Не визначено прилад."
        }

        dao.deleteDevice(ownerKey, deviceId)
    }

    override suspend fun saveForecastDays(
        ownerKey: String,
        days: Int
    ) {
        requireOwner(ownerKey)
        dao.saveForecastDays(ownerKey, days)
    }

    override suspend fun loadDemoScenarios(
        refresh: Boolean
    ): List<DemoScenario> {
        return if (refresh) {
            demoSource.refresh()
        } else {
            demoSource.load()
        }
    }

    /**
     * Додає всі позиції прикладу однією транзакцією.
     * Наявні прилади та встановлений користувачем період зберігаються.
     */
    override suspend fun importDemoScenario(
        ownerKey: String,
        scenarioId: String
    ) {
        requireOwner(ownerKey)

        val scenario = requireNotNull(
            loadDemoScenarios().firstOrNull { it.id == scenarioId }
        ) {
            "Приклад не знайдено."
        }

        require(scenario.devices.size in 1..MAX_DEVICE_POSITIONS) {
            "Некоректна кількість позицій у прикладі."
        }

        // Читання джерела та перевірка не утримують транзакцію Room.
        val imported = scenario.devices.map { device ->
            normalizeDevice(
                device.copy(id = UUID.randomUUID().toString())
            )
        }

        database.withTransaction {
            val currentCount = dao.observeProfile(ownerKey)
                .first()
                ?.devices
                ?.size
                ?: 0

            require(
                currentCount + imported.size <= MAX_DEVICE_POSITIONS
            ) {
                "Недостатньо місця: дозволено до 50 позицій."
            }

            imported.forEach { device ->
                dao.saveDevice(device.toEntity(ownerKey))
            }
        }
    }

    override fun calculateForecastFromDaily(
        dailyConsumptionKwh: Double,
        forecastDays: Int
    ): ForecastResult {
        return EnergyCalculator.calculateFromDaily(
            dailyConsumptionKwh = dailyConsumptionKwh,
            forecastDays = forecastDays
        )
    }

    override fun calculateForecast(
        devices: List<EnergyDevice>,
        forecastDays: Int
    ): ForecastResult {
        return EnergyCalculator.calculateTotal(
            devices = devices,
            forecastDays = forecastDays
        )
    }

    private fun normalizeDevice(device: EnergyDevice): EnergyDevice {
        val validation = DeviceValidator.validate(
            nameText = device.name,
            powerText = device.powerWatts.toString(),
            hoursText = device.hoursPerDay.toString(),
            countText = device.deviceCount.toString()
        )

        require(device.id.isNotBlank() && validation.parameters != null) {
            "Перевірте параметри приладу."
        }

        return device.copy(name = validation.name)
    }

    private fun requireOwner(ownerKey: String) {
        require(ownerKey.isNotBlank()) {
            "Не визначено власника профілю."
        }
    }

    companion object {
        const val MAX_DEVICE_POSITIONS = 50
    }
}