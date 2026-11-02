package ua.kpi.doctrack.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class RecordStatus {
    ACTIVE,
    DUE_SOON,
    DUE_TODAY,
    EXPIRED,
    OVERDUE,
    PAID
}

/**
 * Обчислює статус перевіреного запису на [today]; статус не зберігається в моделі.
 * Термін включає останній день. DUE_SOON означає від 1 до 7 календарних днів.
 * Оплачений рахунок залишається PAID незалежно від дати оплати за планом.
 */
fun DocumentRecord.statusOn(today: LocalDate): RecordStatus {
    if (type == RecordType.BILL && isPaid) return RecordStatus.PAID

    val daysUntilDeadline = ChronoUnit.DAYS.between(today, deadline)
    return when {
        daysUntilDeadline < 0 -> {
            if (type == RecordType.BILL) RecordStatus.OVERDUE else RecordStatus.EXPIRED
        }
        daysUntilDeadline == 0L -> RecordStatus.DUE_TODAY
        daysUntilDeadline <= 7L -> RecordStatus.DUE_SOON
        else -> RecordStatus.ACTIVE
    }
}
