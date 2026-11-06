package ua.kpi.doctrack.ui.form

import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import ua.kpi.doctrack.model.DocumentRecord
import ua.kpi.doctrack.model.RecordDates
import ua.kpi.doctrack.model.RecordCurrency
import ua.kpi.doctrack.model.RecordType
import ua.kpi.doctrack.model.RecordValidationError
import ua.kpi.doctrack.model.RecordValidator

data class RecordFormInput(
    val title: String = "",
    val type: RecordType = RecordType.DOCUMENT,
    val issuer: String = "",
    val issuedOn: String = "",
    val deadline: String = "",
    val amount: String = "",
    val currency: RecordCurrency = RecordCurrency.EUR,
    val isPaid: Boolean = false,
    val note: String = ""
) {
    companion object {
        fun from(record: DocumentRecord?): RecordFormInput =
            if (record == null) RecordFormInput() else RecordFormInput(
                title = record.title,
                type = record.type,
                issuer = record.issuer.orEmpty(),
                issuedOn = record.issuedOn?.format(RecordDates.formatter).orEmpty(),
                deadline = record.deadline.format(RecordDates.formatter),
                amount = record.amountMinor?.let {
                    BigDecimal.valueOf(it, 2).toPlainString()
                }.orEmpty(),
                currency = record.currency ?: RecordCurrency.EUR,
                isPaid = record.isPaid,
                note = record.note.orEmpty()
            )
    }
}

enum class RecordFormField { TITLE, ISSUED_ON, DEADLINE, AMOUNT, TYPE }

enum class RecordFormError {
    REQUIRED, TITLE_TOO_LONG, INVALID_DATE, DEADLINE_BEFORE_ISSUE_DATE,
    INVALID_AMOUNT, AMOUNT_MUST_BE_POSITIVE, DOCUMENT_HAS_BILL_FIELDS
}

data class RecordFormResult(
    val record: DocumentRecord?,
    val errors: Map<RecordFormField, RecordFormError>
)

/** Точно перетворює десяткове значення на дрібні одиниці; допускається використання як коми, так і крапки. */
fun parseAmountMinor(text: String): Long? {
    val value = text.trim()
    if (value.length > 20 || !Regex("[0-9]+([.,][0-9]{1,2})?").matches(value)) {
        return null
    }
    return runCatching {
        BigDecimal(value.replace(',', '.')).movePointRight(2).longValueExact()
    }.getOrNull()
}

/** Зберігає метадані OCR/зображення та ідентифікаційні дані під час редагування; очищує поля рахунків у документах. */
fun validateRecordForm(
    input: RecordFormInput,
    original: DocumentRecord? = null,
    newId: String = UUID.randomUUID().toString()
): RecordFormResult {
    val errors = mutableMapOf<RecordFormField, RecordFormError>()
    val deadlineText = input.deadline.trim()
    val issuedText = input.issuedOn.trim()
    val deadline = RecordDates.parse(deadlineText)
    val issuedOn = if (issuedText.isEmpty()) null else RecordDates.parse(issuedText)
    if (deadline == null) {
        errors[RecordFormField.DEADLINE] = if (deadlineText.isEmpty()) {
            RecordFormError.REQUIRED
        } else RecordFormError.INVALID_DATE
    }
    if (issuedText.isNotEmpty() && issuedOn == null) {
        errors[RecordFormField.ISSUED_ON] = RecordFormError.INVALID_DATE
    }

    val isBill = input.type == RecordType.BILL
    val amount = if (isBill) parseAmountMinor(input.amount) else null
    if (isBill && input.amount.isNotBlank() && amount == null) {
        errors[RecordFormField.AMOUNT] = RecordFormError.INVALID_AMOUNT
    }

    // Дата-заповнювач дозволяє валідатору моделі повідомляти про інші помилки разом.
    val base = original ?: DocumentRecord(
        id = newId, title = input.title.trim(), type = input.type,
        deadline = deadline ?: LocalDate.MIN
    )
    val candidate = base.copy(
        title = input.title.trim(),
        type = input.type,
        issuer = input.issuer.trim().ifEmpty { null },
        issuedOn = issuedOn,
        deadline = deadline ?: LocalDate.MIN,
        amountMinor = amount,
        currency = if (isBill) input.currency else null,
        isPaid = isBill && input.isPaid,
        note = input.note.trim().ifEmpty { null }
    )

    RecordValidator.validate(candidate).forEach { error ->
        when (error) {
            RecordValidationError.TITLE_REQUIRED ->
                errors.putIfAbsent(RecordFormField.TITLE, RecordFormError.REQUIRED)
            RecordValidationError.TITLE_TOO_LONG ->
                errors.putIfAbsent(RecordFormField.TITLE, RecordFormError.TITLE_TOO_LONG)
            RecordValidationError.DEADLINE_YEAR_OUT_OF_RANGE ->
                errors.putIfAbsent(RecordFormField.DEADLINE, RecordFormError.INVALID_DATE)
            RecordValidationError.ISSUE_DATE_YEAR_OUT_OF_RANGE ->
                errors.putIfAbsent(RecordFormField.ISSUED_ON, RecordFormError.INVALID_DATE)
            RecordValidationError.DEADLINE_BEFORE_ISSUE_DATE ->
                errors.putIfAbsent(
                    RecordFormField.DEADLINE, RecordFormError.DEADLINE_BEFORE_ISSUE_DATE
                )
            RecordValidationError.AMOUNT_REQUIRED,
            RecordValidationError.CURRENCY_REQUIRED ->
                errors.putIfAbsent(RecordFormField.AMOUNT, RecordFormError.REQUIRED)
            RecordValidationError.AMOUNT_MUST_BE_POSITIVE ->
                errors.putIfAbsent(RecordFormField.AMOUNT, RecordFormError.AMOUNT_MUST_BE_POSITIVE)
            RecordValidationError.DOCUMENT_HAS_AMOUNT,
            RecordValidationError.DOCUMENT_HAS_CURRENCY,
            RecordValidationError.DOCUMENT_IS_PAID ->
                errors[RecordFormField.TYPE] = RecordFormError.DOCUMENT_HAS_BILL_FIELDS
        }
    }
    return RecordFormResult(candidate.takeIf { errors.isEmpty() }, errors)
}
