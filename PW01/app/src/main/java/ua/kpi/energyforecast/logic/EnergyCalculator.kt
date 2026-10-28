package ua.kpi.energyforecast.logic

import ua.kpi.energyforecast.model.ForecastParameters
import ua.kpi.energyforecast.model.ForecastResult

/**
 * Обчислює прогноз за сталого режиму роботи приладів.
 * Не залежить від Android або Jetpack Compose.
 */
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
}