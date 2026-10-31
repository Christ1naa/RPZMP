package ua.kpi.doctrack.data.demo

import java.time.LocalDate
import ua.kpi.doctrack.model.DocumentRecord
import ua.kpi.doctrack.model.RecordCurrency
import ua.kpi.doctrack.model.RecordType

fun createDemoRecords(
    today: LocalDate = LocalDate.now()
): List<DocumentRecord> {
    return listOf(
        DocumentRecord(
            id = "demo-document-1",
            title = "Страхування велосипеда",
            type = RecordType.DOCUMENT,
            deadline = today.plusDays(5),
            issuer = "Demo Insurance"
        ),
        DocumentRecord(
            id = "demo-bill-1",
            title = "Інтернет",
            type = RecordType.BILL,
            deadline = today.minusDays(1),
            issuer = "Demo Internet",
            amountMinor = 3550L,
            currency = RecordCurrency.EUR
        ),
        DocumentRecord(
            id = "demo-bill-2",
            title = "Електроенергія",
            type = RecordType.BILL,
            deadline = today.minusDays(3),
            issuer = "Demo Energy",
            amountMinor = 6800L,
            currency = RecordCurrency.EUR,
            isPaid = true
        )
    )
}