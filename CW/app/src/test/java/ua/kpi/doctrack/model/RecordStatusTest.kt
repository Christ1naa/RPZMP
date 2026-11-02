package ua.kpi.doctrack.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test
import ua.kpi.doctrack.data.demo.createDemoRecords

class RecordStatusTest {
    private val today = LocalDate.of(2026, 11, 2)
    private val document = DocumentRecord("Договір", RecordType.DOCUMENT, today)
    private val bill = document.copy(type = RecordType.BILL, amountMinor = 3550, currency = RecordCurrency.EUR)

    @Test fun expiredDocument() = assertEquals(RecordStatus.EXPIRED, document.copy(deadline = today.minusDays(1)).statusOn(today))
    @Test fun overdueBill() = assertEquals(RecordStatus.OVERDUE, bill.copy(deadline = today.minusDays(1)).statusOn(today))
    @Test fun todayIsNotOverdue() {
        listOf(document, bill).forEach { assertEquals(RecordStatus.DUE_TODAY, it.statusOn(today)) }
    }
    @Test fun upcomingWindowIncludesOneAndSevenDays() {
        listOf(document, bill).forEach { record ->
            (1L..7L).forEach { days ->
                assertEquals(RecordStatus.DUE_SOON, record.copy(deadline = today.plusDays(days)).statusOn(today))
            }
        }
    }
    @Test fun eightDaysIsActive() {
        listOf(document, bill).forEach {
            assertEquals(RecordStatus.ACTIVE, it.copy(deadline = today.plusDays(8)).statusOn(today))
        }
    }
    @Test fun paidBillTakesPriorityOverEveryDateStatus() {
        listOf(-1L, 0L, 1L, 7L, 8L).forEach {
            assertEquals(RecordStatus.PAID, bill.copy(deadline = today.plusDays(it), isPaid = true).statusOn(today))
        }
    }
    @Test fun statusChangesWithReferenceDate() {
        assertEquals(RecordStatus.DUE_TODAY, bill.statusOn(today))
        assertEquals(RecordStatus.OVERDUE, bill.statusOn(today.plusDays(1)))
    }
    @Test fun leapDay() = assertEquals(
        RecordStatus.DUE_SOON,
        document.copy(deadline = LocalDate.of(2028, 3, 1)).statusOn(LocalDate.of(2028, 2, 29))
    )
    @Test fun yearBoundary() = assertEquals(
        RecordStatus.DUE_SOON,
        document.copy(deadline = LocalDate.of(2027, 1, 1)).statusOn(LocalDate.of(2026, 12, 31))
    )
    @Test fun extremeDatesDoNotOverflow() {
        assertEquals(RecordStatus.ACTIVE, document.copy(deadline = LocalDate.MAX).statusOn(LocalDate.MIN))
        assertEquals(RecordStatus.EXPIRED, document.copy(deadline = LocalDate.MIN).statusOn(LocalDate.MAX))
    }
    @Test fun demoStatuses() = assertEquals(
        listOf(RecordStatus.DUE_SOON, RecordStatus.OVERDUE, RecordStatus.PAID),
        createDemoRecords(today).map { it.statusOn(today) }
    )
}
