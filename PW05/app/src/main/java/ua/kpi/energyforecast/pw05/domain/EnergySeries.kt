package ua.kpi.energyforecast.pw05.domain

/** Енергія за одну годину, а не миттєва потужність сенсора телефона. */
data class HourlyEnergy(val hour: Int, val kwh: Double) {
    init { require(hour >= 0); require(kwh.isFinite() && kwh >= 0.0) }
}
data class EnergySummary(val totalKwh: Double, val meanKwh: Double, val peakKwh: Double)

object EnergySeries {
    fun summarize(samples: List<HourlyEnergy>): EnergySummary? {
        if (samples.isEmpty()) return null
        require(samples.map { it.hour }.distinct().size == samples.size) { "Повтор години" }
        val total = samples.sumOf { it.kwh }
        require(total.isFinite()) { "Переповнення суми" }
        return EnergySummary(total, total / samples.size, samples.maxOf { it.kwh })
    }

    /** Навчальний прогноз: середня погодинна енергія × горизонт; не ML-прогноз. */
    fun forecast(samples: List<HourlyEnergy>, hours: Int): Double? {
        require(hours in 1..8760)
        val mean = summarize(samples)?.meanKwh ?: return null
        return (mean * hours).also { require(it.isFinite()) }
    }
}
