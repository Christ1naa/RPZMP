package ua.kpi.energyforecast.data

import kotlinx.coroutines.flow.Flow
import ua.kpi.energyforecast.data.source.DemoScenario
import ua.kpi.energyforecast.model.EnergyDevice
import ua.kpi.energyforecast.model.ForecastResult

/** Збережені параметри профілю без залежності від класів Room. */
data class SavedEnergyProfile(
    val devices: List<EnergyDevice> = emptyList(),
    val forecastDays: Int = 30,
    val dailyConsumptionKwh: Double = 0.0
)

/** Об’єднує збережений профіль, джерело прикладів і розрахунок прогнозу. */
interface EnergyRepository {

    fun observeProfile(ownerKey: String): Flow<SavedEnergyProfile>

    suspend fun saveDevice(ownerKey: String, device: EnergyDevice)

    suspend fun deleteDevice(ownerKey: String, deviceId: String)

    suspend fun saveForecastDays(ownerKey: String, days: Int)

    suspend fun loadDemoScenarios(
        refresh: Boolean = false
    ): List<DemoScenario>

    suspend fun importDemoScenario(
        ownerKey: String,
        scenarioId: String
    )

    /** Формує прогноз за добовим підсумком, отриманим зі сховища. */
    fun calculateForecastFromDaily(
        dailyConsumptionKwh: Double,
        forecastDays: Int
    ): ForecastResult

    fun calculateForecast(
        devices: List<EnergyDevice>,
        forecastDays: Int
    ): ForecastResult
}