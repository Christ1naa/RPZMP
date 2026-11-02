package ua.kpi.doctrack.model

enum class RecordValidationError {
    TITLE_REQUIRED,
    DEADLINE_BEFORE_ISSUE_DATE,
    AMOUNT_REQUIRED,
    AMOUNT_MUST_BE_POSITIVE,
    CURRENCY_REQUIRED,
    DOCUMENT_HAS_AMOUNT,
    DOCUMENT_HAS_CURRENCY,
    DOCUMENT_IS_PAID
}

/** Перевіряє дані перед збереженням і повертає всі знайдені помилки для форми. */
object RecordValidator {
    fun validate(record: DocumentRecord): List<RecordValidationError> = buildList {
        if (record.title.isBlank()) add(RecordValidationError.TITLE_REQUIRED)

        val issuedOn = record.issuedOn
        if (issuedOn != null && record.deadline.isBefore(issuedOn)) {
            add(RecordValidationError.DEADLINE_BEFORE_ISSUE_DATE)
        }

        when (record.type) {
            RecordType.BILL -> {
                val amount = record.amountMinor
                if (amount == null) {
                    add(RecordValidationError.AMOUNT_REQUIRED)
                } else if (amount <= 0L) {
                    add(RecordValidationError.AMOUNT_MUST_BE_POSITIVE)
                }
                if (record.currency == null) add(RecordValidationError.CURRENCY_REQUIRED)
            }
            RecordType.DOCUMENT -> {
                if (record.amountMinor != null) add(RecordValidationError.DOCUMENT_HAS_AMOUNT)
                if (record.currency != null) add(RecordValidationError.DOCUMENT_HAS_CURRENCY)
                if (record.isPaid) add(RecordValidationError.DOCUMENT_IS_PAID)
            }
        }
    }
}
