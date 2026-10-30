package ua.kpi.energyforecast.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ua.kpi.energyforecast.data.local.EnergyDatabase
import ua.kpi.energyforecast.data.local.toEntity
import ua.kpi.energyforecast.data.local.toModel
import ua.kpi.energyforecast.logic.DeviceValidator
import ua.kpi.energyforecast.model.EnergyDevice

/** Перевіряє дані перед записом і перетворює сутності Room на моделі застосунку. */
class RoomEnergyRepository(
    private val database: EnergyDatabase
) : EnergyRepository {

    private val dao = database.energyDao()

    override fun observeProfile(
        ownerKey: String
    ): Flow<SavedEnergyProfile> {
        return dao.observeProfile(ownerKey).map { saved ->
            SavedEnergyProfile(
                devices = saved?.devices
                    ?.map { it.toModel() }
                    ?.sortedBy { it.id }
                    .orEmpty(),
                forecastDays = saved?.profile?.forecastDays ?: 30
            )
        }
    }

    override suspend fun saveDevice(
        ownerKey: String,
        device: EnergyDevice
    ) {
        require(ownerKey.isNotBlank()) {
            "Не визначено власника профілю."
        }

        val validation = DeviceValidator.validate(
            nameText = device.name,
            powerText = device.powerWatts.toString(),
            hoursText = device.hoursPerDay.toString(),
            countText = device.deviceCount.toString()
        )

        require(device.id.isNotBlank() && validation.parameters != null) {
            "Перевірте параметри приладу."
        }

        // Перевірка ліміту та запис належать одній транзакції.
        database.withTransaction {
            val current = dao.observeProfile(ownerKey)
                .first()
                ?.devices
                .orEmpty()

            val exists = current.any { it.id == device.id }

            require(exists || current.size < MAX_DEVICE_POSITIONS) {
                "Досягнуто межі: 50 позицій."
            }

            dao.saveDevice(
                device.copy(name = validation.name).toEntity(ownerKey)
            )
        }
    }

    override suspend fun deleteDevice(
        ownerKey: String,
        deviceId: String
    ) {
        dao.deleteDevice(ownerKey, deviceId)
    }

    override suspend fun saveForecastDays(
        ownerKey: String,
        days: Int
    ) {
        dao.saveForecastDays(ownerKey, days)
    }

    companion object {
        const val MAX_DEVICE_POSITIONS = 50
    }
}