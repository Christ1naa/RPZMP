package ua.kpi.doctrack.model

enum class RecordValidationError {
    TITLE_REQUIRED,
    TITLE_TOO_LONG,
    DEADLINE_YEAR_OUT_OF_RANGE,
    ISSUE_DATE_YEAR_OUT_OF_RANGE,
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
    const val MAX_TITLE_LENGTH = 120

    fun validate(record: DocumentRecord): List<RecordValidationError> = buildList {
        if (record.title.isBlank()) add(RecordValidationError.TITLE_REQUIRED)

        if (record.title.trim().length > MAX_TITLE_LENGTH) {
            add(RecordValidationError.TITLE_TOO_LONG)
        }
        if (!RecordDates.isSupported(record.deadline)) {
            add(RecordValidationError.DEADLINE_YEAR_OUT_OF_RANGE)
        }

        val issuedOn = record.issuedOn
        if (issuedOn != null && !RecordDates.isSupported(issuedOn)) {
            add(RecordValidationError.ISSUE_DATE_YEAR_OUT_OF_RANGE)
        }
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
