package ua.kpi.energyforecast.pw05.domain

import kotlin.math.ceil
import kotlin.math.hypot

enum class ChartPeriod(val title: String) { DAY("Доба"), WEEK("Тиждень"), MONTH("30 діб") }
data class ChartPoint(val label: String, val kwh: Double) {
    init { require(kwh.isFinite() && kwh >= 0) }
}
object ChartData {
    /** Агрегація одного погодинного джерела: не генеруємо незалежні суми для різних інтервалів. */
    fun aggregate(history: List<HourlyEnergy>, period: ChartPeriod): List<ChartPoint> {
        require(history.map { it.hour }.distinct().size == history.size)
        val hours = when (period) { ChartPeriod.DAY -> 24; ChartPeriod.WEEK -> 168; ChartPeriod.MONTH -> 720 }
        val recent = history.sortedBy { it.hour }.takeLast(hours)
        if (period == ChartPeriod.DAY) return recent.map { ChartPoint("${it.hour % 24}:00", it.kwh) }
        return recent.groupBy { it.hour / 24 }.map { (day, data) -> ChartPoint("Доба ${day + 1}", data.sumOf { it.kwh }) }
    }
    fun window(points: List<ChartPoint>, zoom: Int, offset: Int): List<ChartPoint> {
        require(zoom in 1..4)
        if (points.isEmpty()) return emptyList()
        val count = ceil(points.size.toDouble() / zoom).toInt().coerceAtLeast(1)
        val start = offset.coerceIn(0, points.size - count)
        return points.subList(start, start + count)
    }
    fun summary(points: List<ChartPoint>): EnergySummary? =
        if (points.isEmpty()) null else EnergySeries.summarize(points.mapIndexed { i, p -> HourlyEnergy(i, p.kwh) })
}
/** Нормована XY-проєкція в межах одиничного кола; анімація не виходить за Canvas.
 * Це візуалізація прискорення, а не калібрований кут нахилу. */
fun normalizedVector(x: Float, y: Float, fullScale: Float, scale: Float): Pair<Float, Float> {
    require(fullScale.isFinite() && fullScale > 0 && scale.isFinite() && scale in 0.5f..2f)
    if (!x.isFinite() || !y.isFinite()) return 0f to 0f
    val nx = x.toDouble() / fullScale * scale
    val ny = y.toDouble() / fullScale * scale
    val divisor = hypot(nx, ny).coerceAtLeast(1.0)
    return (nx / divisor).toFloat() to (ny / divisor).toFloat()
}

