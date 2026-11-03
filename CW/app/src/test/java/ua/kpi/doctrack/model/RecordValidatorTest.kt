package ua.kpi.doctrack.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.kpi.doctrack.data.demo.createDemoRecords

class RecordValidatorTest {
    private val date = LocalDate.of(2026, 11, 2)
    private val document = DocumentRecord("Страхування", RecordType.DOCUMENT, date)
    private val bill = document.copy(type = RecordType.BILL, amountMinor = 1L, currency = RecordCurrency.EUR)

    @Test fun validDocument() = assertTrue(RecordValidator.validate(document).isEmpty())
    @Test fun smallestBillAmount() = assertTrue(RecordValidator.validate(bill).isEmpty())
    @Test fun allSupportedCurrencies() {
        RecordCurrency.values().forEach {
            assertTrue(RecordValidator.validate(bill.copy(currency = it)).isEmpty())
        }
    }
    @Test fun blankTitles() {
        listOf("", " ", "\t\n", "\u00A0").forEach {
            errors(document.copy(title = it), RecordValidationError.TITLE_REQUIRED)
        }
    }
    @Test fun titleWithSurroundingWhitespace() =
        assertTrue(RecordValidator.validate(document.copy(title = "  Договір  ")).isEmpty())
    @Test fun deadlineBeforeIssue() = errors(
        document.copy(issuedOn = date.plusDays(1)), RecordValidationError.DEADLINE_BEFORE_ISSUE_DATE
    )
    @Test fun deadlineEqualsIssue() =
        assertTrue(RecordValidator.validate(document.copy(issuedOn = date)).isEmpty())
    @Test fun deadlineAfterIssue() =
        assertTrue(RecordValidator.validate(document.copy(issuedOn = date.minusDays(1))).isEmpty())
    @Test fun historicalRecordIsAllowed() = assertTrue(
        RecordValidator.validate(bill.copy(issuedOn = LocalDate.of(2020, 1, 1), deadline = LocalDate.of(2020, 2, 1))).isEmpty()
    )
    @Test fun missingAmount() = errors(bill.copy(amountMinor = null), RecordValidationError.AMOUNT_REQUIRED)
    @Test fun zeroAmount() = errors(bill.copy(amountMinor = 0), RecordValidationError.AMOUNT_MUST_BE_POSITIVE)
    @Test fun negativeAmounts() {
        listOf(-1L, Long.MIN_VALUE).forEach {
            errors(bill.copy(amountMinor = it), RecordValidationError.AMOUNT_MUST_BE_POSITIVE)
        }
    }
    @Test fun largestRepresentableAmount() =
        assertTrue(RecordValidator.validate(bill.copy(amountMinor = Long.MAX_VALUE)).isEmpty())
    @Test fun missingCurrency() = errors(bill.copy(currency = null), RecordValidationError.CURRENCY_REQUIRED)
    @Test fun documentCannotHaveAmount() = errors(document.copy(amountMinor = 1), RecordValidationError.DOCUMENT_HAS_AMOUNT)
    @Test fun documentCannotHaveCurrency() = errors(document.copy(currency = RecordCurrency.UAH), RecordValidationError.DOCUMENT_HAS_CURRENCY)
    @Test fun documentCannotBePaid() = errors(document.copy(isPaid = true), RecordValidationError.DOCUMENT_IS_PAID)
    @Test fun paidBillStillNeedsAmount() = errors(bill.copy(isPaid = true, amountMinor = null), RecordValidationError.AMOUNT_REQUIRED)
    @Test fun returnsAllErrors() = errors(
        bill.copy(title = "", issuedOn = date.plusDays(1), amountMinor = null, currency = null),
        RecordValidationError.TITLE_REQUIRED, RecordValidationError.DEADLINE_BEFORE_ISSUE_DATE,
        RecordValidationError.AMOUNT_REQUIRED, RecordValidationError.CURRENCY_REQUIRED
    )
    @Test fun demoRecordsAreValid() {
        createDemoRecords(date).forEach { assertTrue(RecordValidator.validate(it).isEmpty()) }
    }

    @Test fun titleLimitAppliesToDocumentsAndBills() {
        listOf(document, bill).forEach { record ->
            assertTrue(RecordValidator.validate(record.copy(title = " " + "a".repeat(120) + " ")).isEmpty())
            errors(record.copy(title = "a".repeat(121)), RecordValidationError.TITLE_TOO_LONG)
        }
    }

    @Test fun rejectsUnsupportedDeadlineYears() {
        listOf(-1, 0, 10000).forEach { year ->
            errors(document.copy(deadline = LocalDate.of(year, 1, 1)),
                RecordValidationError.DEADLINE_YEAR_OUT_OF_RANGE)
        }
    }

    @Test fun rejectsUnsupportedIssueYears() {
        listOf(-1, 0, 10000).forEach { year ->
            val result = RecordValidator.validate(document.copy(issuedOn = LocalDate.of(year, 1, 1)))
            assertTrue(result.contains(RecordValidationError.ISSUE_DATE_YEAR_OUT_OF_RANGE))
        }
    }

    private fun errors(record: DocumentRecord, vararg expected: RecordValidationError) {
        assertEquals(expected.toList(), RecordValidator.validate(record))
    }
}
