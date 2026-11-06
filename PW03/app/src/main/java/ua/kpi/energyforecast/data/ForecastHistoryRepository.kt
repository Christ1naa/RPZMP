package ua.kpi.energyforecast.data

import kotlinx.coroutines.flow.Flow
import ua.kpi.energyforecast.model.EnergyDevice
import ua.kpi.energyforecast.model.SavedForecast

/** Постійне зберігання незалежних копій прогнозів. */
interface ForecastHistoryRepository {

    fun observeHistory(
        ownerKey: String
    ): Flow<List<SavedForecast>>

    fun observeForecast(
        ownerKey: String,
        forecastId: String
    ): Flow<SavedForecast?>

    suspend fun saveForecast(
        ownerKey: String,
        title: String,
        devices: List<EnergyDevice>,
        forecastDays: Int
    ): String

    suspend fun deleteForecast(
        ownerKey: String,
        forecastId: String
    )
}