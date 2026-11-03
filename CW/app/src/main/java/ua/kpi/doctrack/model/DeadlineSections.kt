package ua.kpi.doctrack.model

import java.time.LocalDate

enum class DeadlineGroup { PAST, TODAY, NEXT_SEVEN_DAYS, LATER }

data class DeadlineSection(
    val group: DeadlineGroup,
    val records: List<DocumentRecord>
)

/** Groups actionable deadlines using the same date rules as record status labels. */
fun groupDeadlineRecords(
    records: List<DocumentRecord>,
    today: LocalDate
): List<DeadlineSection> {
    val grouped = records.filterNot { it.type == RecordType.BILL && it.isPaid }
        .sortedWith(compareBy<DocumentRecord> { it.deadline }.thenBy { it.title }.thenBy { it.id })
        .groupBy { record ->
            when (record.statusOn(today)) {
                RecordStatus.EXPIRED, RecordStatus.OVERDUE -> DeadlineGroup.PAST
                RecordStatus.DUE_TODAY -> DeadlineGroup.TODAY
                RecordStatus.DUE_SOON -> DeadlineGroup.NEXT_SEVEN_DAYS
                RecordStatus.ACTIVE -> DeadlineGroup.LATER
                RecordStatus.PAID -> error("Paid bills are excluded before grouping")
            }
        }

    return DeadlineGroup.entries.mapNotNull { group ->
        grouped[group]?.let { DeadlineSection(group, it) }
    }
}
