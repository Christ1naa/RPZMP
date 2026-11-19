package ua.kpi.energyforecast.pw05.domain

import org.junit.Assert.*
import org.junit.Test

class AdvancedFeaturesTest {
    private val alert = Alert(7, AlertCategory.REMINDER, "Подія", "Текст", 1000)
    @Test fun snoozeReschedulesAndInvalidatesOldDelivery() {
        val delayed = AlertRules.apply(alert, AlertAction.SNOOZE, 2000)
        assertEquals(302000, delayed.due)
        assertEquals(1, delayed.revision)
        assertFalse(AlertRules.deliverable(delayed, 0, 400000))
        assertFalse(AlertRules.deliverable(delayed, 1, 301999))
        assertTrue(AlertRules.deliverable(delayed, 1, 302000))
        assertEquals(alert.id, delayed.id)
    }
    @Test fun terminalActionsAreIdempotent() {
        for (action in listOf(AlertAction.CONFIRM, AlertAction.CANCEL)) {
            val done = AlertRules.apply(alert, action, 1000)
            assertFalse(AlertRules.deliverable(done, done.revision, 900000))
            AlertAction.entries.forEach { assertEquals(done, AlertRules.apply(done, it, 2000)) }
        }
    }
    @Test fun independentIdsAndCategoriesRemainIndependent() {
        val second = alert.copy(id = 8, category = AlertCategory.FORECAST)
        val updated = listOf(alert, second).map { if(it.id == 7) AlertRules.apply(it, AlertAction.CANCEL, 2000) else it }
        assertEquals(second, updated[1])
        assertEquals(AlertStatus.CANCELLED, updated[0].status)
    }
    @Test fun blockedCanBeExplicitlyRescheduled() {
        val blocked = alert.copy(status = AlertStatus.BLOCKED)
        assertFalse(AlertRules.deliverable(blocked, 0, 1000))
        assertEquals(AlertStatus.SCHEDULED, AlertRules.apply(blocked, AlertAction.SNOOZE, 1000).status)
    }
    @Test fun aggregationPreservesSameSourceTotals() {
        val history = (0 until 720).map { HourlyEnergy(it, 1.0) }
        assertEquals(24.0, ChartData.aggregate(history, ChartPeriod.DAY).sumOf { it.kwh }, 0.0)
        assertEquals(168.0, ChartData.aggregate(history, ChartPeriod.WEEK).sumOf { it.kwh }, 0.0)
        val month = ChartData.aggregate(history, ChartPeriod.MONTH)
        assertEquals(30, month.size)
        assertEquals(720.0, month.sumOf { it.kwh }, 0.0)
        assertEquals(24.0, ChartData.summary(month)!!.meanKwh, 0.0)
    }
    @Test fun aggregationSortsAndRejectsDuplicateHours() {
        val data = listOf(HourlyEnergy(2, 3.0), HourlyEnergy(1, 2.0))
        assertEquals(listOf("1:00", "2:00"), ChartData.aggregate(data, ChartPeriod.DAY).map { it.label })
        assertThrows(IllegalArgumentException::class.java) { ChartData.aggregate(data + data, ChartPeriod.DAY) }
    }
    @Test fun zoomClampsOffsetAndComputesVisibleStatistics() {
        val data = (1..10).map { ChartPoint(it.toString(), it.toDouble()) }
        val visible = ChartData.window(data, 4, 99)
        assertEquals(listOf(8.0, 9.0, 10.0), visible.map { it.kwh })
        assertEquals(9.0, ChartData.summary(visible)!!.meanKwh, 0.0)
        assertEquals(10.0, ChartData.summary(visible)!!.peakKwh, 0.0)
        assertEquals(3, ChartData.window(data, 4, -1).size)
    }
    @Test fun emptyAndZeroChartDataAreDistinct() {
        assertNull(ChartData.summary(emptyList()))
        assertEquals(emptyList<ChartPoint>(), ChartData.window(emptyList(), 1, 0))
        assertEquals(0.0, ChartData.summary(listOf(ChartPoint("0", 0.0)))!!.totalKwh, 0.0)
        assertThrows(IllegalArgumentException::class.java) { ChartData.window(emptyList(), 0, 0) }
        assertThrows(IllegalArgumentException::class.java) { ChartPoint("bad", Double.NaN) }
    }
    @Test fun normalizedVectorsStayInCircleEvenForExtremeInputs() {
        val (x, y) = normalizedVector(Float.MAX_VALUE, Float.MAX_VALUE, 1f, 2f)
        assertTrue(x.isFinite() && y.isFinite())
        assertEquals(1.0, kotlin.math.hypot(x.toDouble(), y.toDouble()), 0.00001)
        assertEquals(0f to 0f, normalizedVector(Float.NaN, 1f, 1f, 1f))
        assertEquals(0.5f to 0f, normalizedVector(1f, 0f, 2f, 1f))
        assertThrows(IllegalArgumentException::class.java) { normalizedVector(1f, 1f, 0f, 1f) }
        assertThrows(IllegalArgumentException::class.java) { normalizedVector(1f, 1f, 1f, 3f) }
    }
}

