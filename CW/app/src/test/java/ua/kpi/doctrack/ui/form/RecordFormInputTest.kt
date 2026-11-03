package ua.kpi.doctrack.ui.form

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
import ua.kpi.doctrack.model.DocumentRecord
import ua.kpi.doctrack.model.RecordCurrency
import ua.kpi.doctrack.model.RecordType

class RecordFormInputTest {
    private fun validInput() = RecordFormInput(title = "Test", deadline = "03.11.2026")

    @Test fun acceptsCommaAndDotWithoutRounding() {
        assertEquals(3550L, parseAmountMinor("35,50"))
        assertEquals(3550L, parseAmountMinor("35.5"))
        assertEquals(3500L, parseAmountMinor(" 35 "))
        assertEquals(1L, parseAmountMinor("0.01"))
    }

    @Test fun rejectsMalformedAndOverPreciseAmounts() {
        listOf("", "1.234", "1,2.3", "1e3", "-1", "NaN", ".5").forEach {
            assertNull(it, parseAmountMinor(it))
        }
    }

    @Test fun detectsLongOverflowAtMinorUnitBoundary() {
        assertEquals(Long.MAX_VALUE, parseAmountMinor("92233720368547758.07"))
        assertNull(parseAmountMinor("92233720368547758.08"))
        assertNull(parseAmountMinor("999999999999999999999999999999"))
    }

    @Test fun rejectsImpossibleCalendarDates() {
        listOf("31.02.2026", "29.02.2025", "2026-11-03", "3.11.2026").forEach {
            val result = validateRecordForm(validInput().copy(deadline = it))
            assertNull(result.record)
            assertEquals(RecordFormError.INVALID_DATE, result.errors[RecordFormField.DEADLINE])
        }
    }

    @Test fun acceptsLeapDayAndExpiredDates() {
        val result = validateRecordForm(validInput().copy(deadline = "29.02.2024"))
        assertTrue(result.errors.isEmpty())
        assertEquals(LocalDate.of(2024, 2, 29), result.record?.deadline)
    }

    @Test fun returnsRequiredErrorsTogether() {
        val result = validateRecordForm(RecordFormInput(type = RecordType.BILL))
        assertNull(result.record)
        assertEquals(RecordFormError.REQUIRED, result.errors[RecordFormField.TITLE])
        assertEquals(RecordFormError.REQUIRED, result.errors[RecordFormField.DEADLINE])
        assertEquals(RecordFormError.REQUIRED, result.errors[RecordFormField.AMOUNT])
    }

    @Test fun appliesTrimmedTitleLengthBoundary() {
        assertNotNull(validateRecordForm(validInput().copy(title = " " + "a".repeat(120) + " ")).record)
        assertEquals(RecordFormError.TITLE_TOO_LONG,
            validateRecordForm(validInput().copy(title = "a".repeat(121)))
                .errors[RecordFormField.TITLE])
    }

    @Test fun validatesOptionalIssueDateAndDateOrder() {
        assertNotNull(validateRecordForm(validInput().copy(issuedOn = "03.11.2026")).record)
        assertEquals(RecordFormError.DEADLINE_BEFORE_ISSUE_DATE,
            validateRecordForm(validInput().copy(issuedOn = "04.11.2026"))
                .errors[RecordFormField.DEADLINE])
        assertEquals(RecordFormError.INVALID_DATE,
            validateRecordForm(validInput().copy(issuedOn = "bad"))
                .errors[RecordFormField.ISSUED_ON])
    }

    @Test fun reportsMalformedAmountBeforeMissingAmount() {
        val result = validateRecordForm(validInput().copy(type = RecordType.BILL, amount = "1.234"))
        assertEquals(RecordFormError.INVALID_AMOUNT, result.errors[RecordFormField.AMOUNT])
    }

    @Test fun rejectsZeroThroughModelValidator() {
        val result = validateRecordForm(validInput().copy(type = RecordType.BILL, amount = "0"))
        assertEquals(RecordFormError.AMOUNT_MUST_BE_POSITIVE, result.errors[RecordFormField.AMOUNT])
    }

    @Test fun preservesIdentityAndOcrMetadataOnEdit() {
        val original = DocumentRecord(
            id = "original", title = "Old", type = RecordType.BILL,
            deadline = LocalDate.of(2026, 11, 3), amountMinor = 3550,
            currency = RecordCurrency.EUR, imagePath = "image.jpg", recognizedText = "OCR"
        )
        val result = validateRecordForm(RecordFormInput.from(original).copy(title = "New"), original)
        assertEquals("original", result.record?.id)
        assertEquals("image.jpg", result.record?.imagePath)
        assertEquals("OCR", result.record?.recognizedText)
        assertEquals("New", result.record?.title)
    }

    @Test fun switchingBillToDocumentClearsHiddenFinancialFields() {
        val input = validInput().copy(type = RecordType.DOCUMENT, amount = "35.50", isPaid = true)
        val record = validateRecordForm(input).record!!
        assertNull(record.amountMinor)
        assertNull(record.currency)
        assertFalse(record.isPaid)
    }

    @Test fun trimsTextAndUsesProvidedNewIdentity() {
        val record = validateRecordForm(validInput().copy(
            title = " Test ", issuer = "   ", note = " Note "
        ), newId = "new-id").record!!
        assertEquals("new-id", record.id)
        assertEquals("Test", record.title)
        assertNull(record.issuer)
        assertEquals("Note", record.note)
    }
    @Test fun rejectsYearZeroInBothDateFields() {
        assertEquals(RecordFormError.INVALID_DATE,
            validateRecordForm(validInput().copy(deadline = "01.01.0000"))
                .errors[RecordFormField.DEADLINE])
        assertEquals(RecordFormError.INVALID_DATE,
            validateRecordForm(validInput().copy(issuedOn = "01.01.0000"))
                .errors[RecordFormField.ISSUED_ON])
    }

    @Test fun supportedDateBoundariesSurviveFormRoundTrip() {
        val input = validInput().copy(issuedOn = "01.01.0001", deadline = "31.12.9999")
        val record = validateRecordForm(input).record!!
        val restored = RecordFormInput.from(record)
        assertEquals(input.issuedOn, restored.issuedOn)
        assertEquals(input.deadline, restored.deadline)
    }
}
