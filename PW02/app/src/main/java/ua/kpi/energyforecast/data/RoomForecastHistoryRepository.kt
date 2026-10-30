package ua.kpi.energyforecast.data

import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ua.kpi.energyforecast.data.local.EnergyDatabase
import ua.kpi.energyforecast.data.local.ForecastItemEntity
import ua.kpi.energyforecast.data.local.ForecastRecordEntity
import ua.kpi.energyforecast.data.local.ForecastWithItems
import ua.kpi.energyforecast.logic.DeviceValidator
import ua.kpi.energyforecast.logic.EnergyCalculator
import ua.kpi.energyforecast.model.EnergyDevice
import ua.kpi.energyforecast.model.ForecastResult
import ua.kpi.energyforecast.model.SavedForecast

/**
 * Перевіряє копію введених даних і зберігає її разом із результатом.
 * Результат обчислюється тут, тому він відповідає саме збереженим параметрам.
 */
class RoomForecastHistoryRepository(
    database: EnergyDatabase,
    private val now: () -> Long = { System.currentTimeMillis() }
) : ForecastHistoryRepository {

    private val dao = database.forecastHistoryDao()

    override fun observeHistory(
        ownerKey: String
    ): Flow<List<SavedForecast>> {
        requireOwner(ownerKey)

        return dao.observeHistory(ownerKey).map { records ->
            records.map { it.toModel() }
        }
    }

    override fun observeForecast(
        ownerKey: String,
        forecastId: String
    ): Flow<SavedForecast?> {
        requireOwner(ownerKey)
        require(forecastId.isNotBlank()) {
            "Не визначено прогноз."
        }

        return dao.observeForecast(ownerKey, forecastId).map {
            it?.toModel()
        }
    }

    override suspend fun saveForecast(
        ownerKey: String,
        title: String,
        devices: List<EnergyDevice>,
        forecastDays: Int
    ): String {
        requireOwner(ownerKey)

        val normalizedTitle = title.trim()

        require(normalizedTitle.length in 1..60) {
            "Назва прогнозу має містити від 1 до 60 символів."
        }
        require(devices.size in 1..50) {
            "Прогноз має містити від 1 до 50 позицій."
        }
        require(devices.map { it.id }.distinct().size == devices.size) {
            "Ідентифікатори приладів повторюються."
        }

        // Створюємо окремий список до першого запису у сховище.
        val snapshot = devices.map { device ->
            val validation = DeviceValidator.validate(
                nameText = device.name,
                powerText = device.powerWatts.toString(),
                hoursText = device.hoursPerDay.toString(),
                countText = device.deviceCount.toString()
            )

            require(
                device.id.isNotBlank() &&
                        validation.parameters != null
            ) {
                "Перевірте параметри приладів."
            }

            device.copy(name = validation.name)
        }

        val result = EnergyCalculator.calculateTotal(
            devices = snapshot,
            forecastDays = forecastDays
        )

        val forecastId = UUID.randomUUID().toString()

        val record = ForecastRecordEntity(
            id = forecastId,
            ownerKey = ownerKey,
            title = normalizedTitle,
            createdAt = now(),
            forecastDays = result.forecastDays,
            dailyConsumptionKwh = result.dailyConsumptionKwh,
            periodConsumptionKwh = result.periodConsumptionKwh
        )

        val items = snapshot.mapIndexed { index, device ->
            ForecastItemEntity(
                forecastId = forecastId,
                position = index,
                deviceId = device.id,
                name = device.name,
                powerWatts = device.powerWatts,
                hoursPerDay = device.hoursPerDay,
                deviceCount = device.deviceCount
            )
        }

        dao.saveForecast(record, items)

        return forecastId
    }

    override suspend fun deleteForecast(
        ownerKey: String,
        forecastId: String
    ) {
        requireOwner(ownerKey)
        require(forecastId.isNotBlank()) {
            "Не визначено прогноз."
        }

        dao.deleteForecast(ownerKey, forecastId)
    }

    private fun requireOwner(ownerKey: String) {
        require(ownerKey.isNotBlank()) {
            "Не визначено власника профілю."
        }
    }

    private fun ForecastWithItems.toModel(): SavedForecast {
        return SavedForecast(
            id = record.id,
            title = record.title,
            createdAt = record.createdAt,
            result = ForecastResult(
                dailyConsumptionKwh = record.dailyConsumptionKwh,
                periodConsumptionKwh = record.periodConsumptionKwh,
                forecastDays = record.forecastDays
            ),
            devices = items.sortedBy { it.position }.map { item ->
                EnergyDevice(
                    id = item.deviceId,
                    name = item.name,
                    powerWatts = item.powerWatts,
                    hoursPerDay = item.hoursPerDay,
                    deviceCount = item.deviceCount
                )
            }
        )
    }
}