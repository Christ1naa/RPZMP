package ua.kpi.energyforecast.data

import kotlinx.coroutines.flow.Flow
import ua.kpi.energyforecast.model.EnergyDevice

/** Збережені параметри профілю без залежності від класів Room. */
data class SavedEnergyProfile(
    val devices: List<EnergyDevice> = emptyList(),
    val forecastDays: Int = 30
)

/** Контракт сховища, який можна замінити тестовою реалізацією. */
interface EnergyRepository {
    fun observeProfile(ownerKey: String): Flow<SavedEnergyProfile>

    suspend fun saveDevice(ownerKey: String, device: EnergyDevice)

    suspend fun deleteDevice(ownerKey: String, deviceId: String)

    suspend fun saveForecastDays(ownerKey: String, days: Int)
}