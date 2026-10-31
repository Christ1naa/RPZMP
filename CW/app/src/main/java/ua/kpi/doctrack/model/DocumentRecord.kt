package ua.kpi.doctrack.model

import java.time.LocalDate
import java.util.UUID

enum class RecordType {
    DOCUMENT,
    BILL
}

enum class RecordCurrency {
    EUR,
    UAH,
    USD
}

/**
 * Запис про документ або рахунок.
 *
 * Грошова сума задається в найменших одиницях валюти:
 * наприклад, 3550 означає 35,50 EUR.
 * Поля суми, валюти та оплати застосовуються лише до рахунків.
 */
data class DocumentRecord(
    val title: String,
    val type: RecordType,
    val deadline: LocalDate,
    val id: String = UUID.randomUUID().toString(),
    val issuer: String? = null,
    val issuedOn: LocalDate? = null,
    val amountMinor: Long? = null,
    val currency: RecordCurrency? = null,
    val isPaid: Boolean = false,
    val note: String? = null,
    val imagePath: String? = null,
    val recognizedText: String? = null
)