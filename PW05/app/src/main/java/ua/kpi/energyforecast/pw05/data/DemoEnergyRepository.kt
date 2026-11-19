package ua.kpi.energyforecast.pw05.data

import ua.kpi.energyforecast.pw05.domain.HourlyEnergy

/** Детерміновані приклади для відтворення тестів; не реальні вимірювання. */
class DemoEnergyRepository {
    fun dailyHistory(): List<HourlyEnergy> = (0..23).map { hour ->
        HourlyEnergy(hour, when (hour) { in 0..5 -> 0.4; in 6..8 -> 1.2; in 9..16 -> 0.8; else -> 1.6 })
    }
}
