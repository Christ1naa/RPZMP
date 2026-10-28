package ua.kpi.energyforecast.logic

import ua.kpi.energyforecast.model.ForecastParameters
import ua.kpi.energyforecast.model.ForecastResult
import ua.kpi.energyforecast.model.EnergyDevice

object EnergyCalculator {

    fun calculate(parameters: ForecastParameters): ForecastResult {
        require(
            parameters.powerWatts.isFinite() &&
                    parameters.powerWatts > 0
        ) {
            "Потужність має бути скінченним додатним числом"
        }

        require(
            parameters.hoursPerDay.isFinite() &&
                    parameters.hoursPerDay in 0.0..24.0
        ) {
            "Час роботи має бути від 0 до 24 годин"
        }

        require(parameters.deviceCount > 0) {
            "Кількість приладів має бути додатною"
        }

        require(parameters.forecastDays > 0) {
            "Період прогнозування має бути додатним"
        }

        val dailyConsumption =
            parameters.powerWatts / 1000.0 *
                    parameters.hoursPerDay *
                    parameters.deviceCount

        val periodConsumption =
            dailyConsumption * parameters.forecastDays

        require(
            dailyConsumption.isFinite() &&
                    periodConsumption.isFinite()
        ) {
            "Результат розрахунку завеликий"
        }

        return ForecastResult(
            dailyConsumptionKwh = dailyConsumption,
            periodConsumptionKwh = periodConsumption,
            forecastDays = parameters.forecastDays
        )
    }

    fun calculateDeviceDaily(device: EnergyDevice): Double {
        return calculate(
            ForecastParameters(
                powerWatts = device.powerWatts,
                hoursPerDay = device.hoursPerDay,
                deviceCount = device.deviceCount,
                forecastDays = 1
            )
        ).dailyConsumptionKwh
    }

    fun calculateTotal(
        devices: List<EnergyDevice>,
        forecastDays: Int
    ): ForecastResult {
        require(forecastDays in 1..365) {
            "Період прогнозування має бути від 1 до 365 днів"
        }

        val dailyConsumption = devices.sumOf { device ->
            calculateDeviceDaily(device)
        }

        val periodConsumption = dailyConsumption * forecastDays

        require(
            dailyConsumption.isFinite() &&
                    periodConsumption.isFinite()
        ) {
            "Результат розрахунку завеликий"
        }

        return ForecastResult(
            dailyConsumptionKwh = dailyConsumption,
            periodConsumptionKwh = periodConsumption,
            forecastDays = forecastDays
        )
    }
}