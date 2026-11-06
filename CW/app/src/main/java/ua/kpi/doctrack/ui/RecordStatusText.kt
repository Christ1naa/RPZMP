package ua.kpi.doctrack.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import java.time.LocalDate
import ua.kpi.doctrack.R
import ua.kpi.doctrack.model.DocumentRecord
import ua.kpi.doctrack.model.RecordStatus
import ua.kpi.doctrack.model.RecordType
import ua.kpi.doctrack.model.statusOn

/** Використовує ідентичні формулювання статусів і семантичні кольори в картках та деталях записів. */
@Composable
internal fun RecordStatusText(
    record: DocumentRecord,
    today: LocalDate,
    style: TextStyle = MaterialTheme.typography.labelLarge
) {
    val status = record.statusOn(today)
    val isBill = record.type == RecordType.BILL
    val label = when (status) {
        RecordStatus.ACTIVE ->
            if (isBill) R.string.status_bill_active else R.string.status_active
        RecordStatus.DUE_SOON ->
            if (isBill) R.string.status_bill_due_soon else R.string.status_due_soon
        RecordStatus.DUE_TODAY ->
            if (isBill) R.string.status_bill_due_today else R.string.status_due_today
        RecordStatus.EXPIRED -> R.string.status_expired
        RecordStatus.OVERDUE -> R.string.status_overdue
        RecordStatus.PAID -> R.string.record_paid
    }
    val color = when (status) {
        RecordStatus.EXPIRED, RecordStatus.OVERDUE -> MaterialTheme.colorScheme.error
        RecordStatus.DUE_TODAY, RecordStatus.DUE_SOON -> MaterialTheme.colorScheme.tertiary
        RecordStatus.PAID, RecordStatus.ACTIVE -> MaterialTheme.colorScheme.primary
    }
    Text(text = stringResource(label), style = style, color = color)
}
