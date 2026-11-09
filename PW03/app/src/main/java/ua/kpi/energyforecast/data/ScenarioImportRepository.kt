package ua.kpi.energyforecast.data

import androidx.room.withTransaction
import java.util.UUID
import kotlinx.coroutines.flow.first
import ua.kpi.energyforecast.data.local.EnergyDatabase
import ua.kpi.energyforecast.data.local.toEntity
import ua.kpi.energyforecast.logic.DeviceValidator

/**
 * Переносить прилади з каталогу сценаріїв до особистого профілю.
 * Період профілю та наявні прилади залишаються незмінними.
 */
class ScenarioImportRepository(
    private val database: EnergyDatabase,
    private val scenarios: ScenarioRepository
) {

    suspend fun importScenario(
        ownerKey: String,
        scenarioId: String
    ) {
        require(ownerKey.isNotBlank()) {
            "Не визначено власника профілю."
        }

        require(scenarioId.isNotBlank()) {
            "Не визначено сценарій."
        }

        val scenario = requireNotNull(
            scenarios.getCachedScenario(scenarioId)
        ) {
            "Сценарій відсутній у збереженому каталозі. Оновіть каталог."
        }

        require(
            scenario.devices.size in
                    1..RoomEnergyRepository.MAX_DEVICE_POSITIONS
        ) {
            "Некоректна кількість позицій у сценарії."
        }

        // Підготовка й перевірка не утримують транзакцію бази.
        val importedDevices = scenario.devices.map { device ->
            val validation = DeviceValidator.validate(
                nameText = device.name,
                powerText = device.powerWatts.toString(),
                hoursText = device.hoursPerDay.toString(),
                countText = device.deviceCount.toString()
            )

            require(validation.parameters != null) {
                "Сценарій містить некоректні параметри приладу."
            }

            device.copy(
                id = UUID.randomUUID().toString(),
                name = validation.name
            )
        }

        val dao = database.energyDao()

        database.withTransaction {
            val currentCount = dao.observeProfile(ownerKey)
                .first()
                ?.devices
                ?.size
                ?: 0

            require(
                currentCount + importedDevices.size <=
                        RoomEnergyRepository.MAX_DEVICE_POSITIONS
            ) {
                "Недостатньо місця: дозволено до 50 позицій."
            }

            importedDevices.forEach { device ->
                dao.saveDevice(device.toEntity(ownerKey))
            }
        }
    }
}