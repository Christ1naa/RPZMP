package ua.kpi.energyforecast.logic

import ua.kpi.energyforecast.model.ForecastParameters
import ua.kpi.energyforecast.model.ForecastResult
import ua.kpi.energyforecast.model.EnergyDevice

/** Розрахункова оцінка за сталої потужності та однакового режиму роботи щодня. */
object EnergyCalculator {

    /**
     * Для групи: E_day = P / 1000 × t × q, E_period = E_day × D.
     * P — вати одного приладу, t — години на добу, q — кількість, D — дні; результат у кВт·год.
     * Нуль годин допустимий; нечислові, нескінченні та переповнені результати відхиляються.
     * Верхні межі введення задає ForecastValidator; тут перевіряється математичний контракт.
     */
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

    /** Обчислює добову енергію всієї групи з урахуванням deviceCount, без округлення. */
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

    /**
     * Підсумовує добову енергію груп, потім множить на період 1–365 днів.
     * Порожній список дає нуль; перевірка скінченності захищає також суму та добуток.
     */
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

        return calculateFromDaily(dailyConsumption, forecastDays)
    }

    /**
     * Використовує готовий добовий підсумок, зокрема обчислений у Room.
     * Період перевіряється до множення; округлення виконує лише UI.
     */
    fun calculateFromDaily(
        dailyConsumptionKwh: Double,
        forecastDays: Int
    ): ForecastResult {
        require(forecastDays in 1..365) {
            "Період прогнозування має бути від 1 до 365 днів"
        }
        require(dailyConsumptionKwh.isFinite() && dailyConsumptionKwh >= 0.0) {
            "Добове споживання має бути скінченним невід’ємним числом"
        }

        val periodConsumption = dailyConsumptionKwh * forecastDays

        require(periodConsumption.isFinite()) {
            "Результат розрахунку завеликий"
        }

        return ForecastResult(
            dailyConsumptionKwh = dailyConsumptionKwh,
            periodConsumptionKwh = periodConsumption,
            forecastDays = forecastDays
        )
    }
}
