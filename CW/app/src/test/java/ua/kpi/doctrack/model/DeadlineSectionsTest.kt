package ua.kpi.doctrack.model

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class DeadlineSectionsTest {
    private val today = LocalDate.of(2026, 11, 3)

    private fun record(id: String, days: Long, paid: Boolean = false,
        type: RecordType = RecordType.BILL) = DocumentRecord(
        id = id, title = id, type = type, deadline = today.plusDays(days),
        amountMinor = if (type == RecordType.BILL) 100L else null,
        currency = if (type == RecordType.BILL) RecordCurrency.EUR else null,
        isPaid = paid
    )

    @Test fun groupsBoundaryDatesInUrgencyOrder() {
        val records = listOf(record("eight", 8), record("seven", 7), record("today", 0),
            record("yesterday", -1), record("one", 1))
        val sections = groupDeadlineRecords(records, today)
        assertEquals(DeadlineGroup.entries.toList(), sections.map { it.group })
        assertEquals(listOf(listOf("yesterday"), listOf("today"), listOf("one", "seven"),
            listOf("eight")), sections.map { section -> section.records.map { it.id } })
    }

    @Test fun excludesPaidBillsRegardlessOfDate() {
        val records = listOf(-5L, 0L, 1L, 7L, 8L).map { record("paid$it", it, paid = true) }
        assertTrue(groupDeadlineRecords(records, today).isEmpty())
    }

    @Test fun keepsExpiredDocumentsAndUnpaidBills() {
        val document = record("document", -3, type = RecordType.DOCUMENT)
        val bill = record("bill", -1)
        assertEquals(listOf(document, bill),
            groupDeadlineRecords(listOf(bill, document), today).single().records)
    }

    @Test fun omitsEmptySectionsAndHandlesEmptyInput() {
        assertTrue(groupDeadlineRecords(emptyList(), today).isEmpty())
        assertEquals(listOf(DeadlineGroup.LATER),
            groupDeadlineRecords(listOf(record("later", 20)), today).map { it.group })
    }

    @Test fun sortsByDateThenTitleAndIdentityWithoutMutatingInput() {
        val a = record("a", 1).copy(title = "Same")
        val b = record("b", 1).copy(title = "Same")
        val first = record("z", 1).copy(title = "First")
        val later = record("later", 2)
        val input = listOf(later, b, first, a)
        assertEquals(listOf(first, a, b, later), groupDeadlineRecords(input, today).single().records)
        assertEquals(listOf(later, b, first, a), input)
    }

    @Test fun reflectsPaymentAndDateEdits() {
        val original = record("bill", 0)
        assertEquals(DeadlineGroup.TODAY, groupDeadlineRecords(listOf(original), today).single().group)
        assertTrue(groupDeadlineRecords(listOf(original.copy(isPaid = true)), today).isEmpty())
        assertEquals(DeadlineGroup.LATER, groupDeadlineRecords(
            listOf(original.copy(deadline = today.plusDays(8))), today).single().group)
    }

    @Test fun regroupingUsesInjectedDateAcrossMonthAndYearBoundaries() {
        val yearEnd = LocalDate.of(2026, 12, 31)
        val record = record("bill", 0).copy(deadline = yearEnd.plusDays(1))
        assertEquals(DeadlineGroup.NEXT_SEVEN_DAYS,
            groupDeadlineRecords(listOf(record), yearEnd).single().group)
        assertEquals(DeadlineGroup.TODAY,
            groupDeadlineRecords(listOf(record), yearEnd.plusDays(1)).single().group)
        assertEquals(DeadlineGroup.PAST,
            groupDeadlineRecords(listOf(record), yearEnd.plusDays(2)).single().group)
    }
}
