package ua.kpi.energyforecast.pw05.domain

import kotlin.math.sqrt

enum class SensorKind(val title: String, val unit: String, val dimensions: Int, val defaultThreshold: Float) {
    ACCELEROMETER("Акселерометр", "м/с²", 3, 12f),
    GYROSCOPE("Гіроскоп", "рад/с", 3, 2f),
    LIGHT("Освітленість", "лк", 1, 500f)
}
data class SensorReading(val kind: SensorKind, val timestampNanos: Long, val values: List<Float>, val magnitude: Float)
object ReadingValidator {
    fun parse(kind: SensorKind, time: Long, values: FloatArray): SensorReading? {
        if (time <= 0 || values.size < kind.dimensions) return null
        val copied = values.take(kind.dimensions)
        if (copied.any { !it.isFinite() } || (kind == SensorKind.LIGHT && copied[0] < 0)) return null
        // Double у сумі квадратів захищає від проміжного переповнення Float.
        val magnitude = if (kind.dimensions == 1) copied[0] else sqrt(copied.sumOf { it.toDouble() * it }).toFloat()
        return if (magnitude.isFinite()) SensorReading(kind, time, copied, magnitude) else null
    }
}
enum class SensorStatus { WAITING, LIVE, STALE, MISSING, FAILED, INVALID }
data class SensorSnapshot(val kind: SensorKind, val status: SensorStatus, val readings: List<SensorReading> = emptyList())
enum class SensorSort { TYPE, VALUE, TIME }
data class SensorQuery(val kind: SensorKind? = null, val minimum: Float? = null, val maximum: Float? = null, val sort: SensorSort = SensorSort.TIME, val descending: Boolean = true) {
    init {
        require(minimum == null || minimum.isFinite())
        require(maximum == null || maximum.isFinite())
        require(minimum == null || maximum == null || minimum <= maximum)
    }
    fun apply(readings: List<SensorReading>): List<SensorReading> {
        val comparator = when (sort) {
            SensorSort.TYPE -> compareBy<SensorReading> { it.kind.ordinal }
            SensorSort.VALUE -> compareBy { it.magnitude }
            SensorSort.TIME -> compareBy { it.timestampNanos }
        }.let { if (descending) it.reversed() else it }.thenBy { it.kind.ordinal }.thenBy { it.timestampNanos }
        return readings.filter { (kind == null || it.kind == kind) && (minimum == null || it.magnitude >= minimum) && (maximum == null || it.magnitude <= maximum) }.sortedWith(comparator)
    }
}
/** Стан порога: подія лише при переході знизу вгору, з гістерезисом 5%. */
class ThresholdGate {
    private var above = false
    fun update(value: Float, threshold: Float): Boolean {
        require(value.isFinite() && threshold.isFinite() && threshold >= 0)
        if (above && value <= threshold * 0.95f) above = false
        if (!above && value > threshold) { above = true; return true }
        return false
    }
}
