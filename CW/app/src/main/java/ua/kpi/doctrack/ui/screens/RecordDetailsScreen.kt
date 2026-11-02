package ua.kpi.doctrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Currency
import ua.kpi.doctrack.R
import ua.kpi.doctrack.model.DocumentRecord
import ua.kpi.doctrack.model.RecordStatus
import ua.kpi.doctrack.model.RecordType
import ua.kpi.doctrack.model.statusOn

private val detailsDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

@Composable
fun RecordDetailsScreen(
    record: DocumentRecord?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.details_back))
        }

        if (record == null) {
            Text(
                text = stringResource(R.string.details_not_found),
                style = MaterialTheme.typography.titleLarge
            )
        } else {
            val isBill = record.type == RecordType.BILL
            val status = record.statusOn(LocalDate.now())
            val statusLabel = when (status) {
                RecordStatus.ACTIVE ->
                    if (isBill) R.string.status_bill_active
                    else R.string.status_active

                RecordStatus.DUE_SOON ->
                    if (isBill) R.string.status_bill_due_soon
                    else R.string.status_due_soon

                RecordStatus.DUE_TODAY ->
                    if (isBill) R.string.status_bill_due_today
                    else R.string.status_due_today

                RecordStatus.EXPIRED -> R.string.status_expired
                RecordStatus.OVERDUE -> R.string.status_overdue
                RecordStatus.PAID -> R.string.record_paid
            }
            val statusColor = when (status) {
                RecordStatus.EXPIRED,
                RecordStatus.OVERDUE -> MaterialTheme.colorScheme.error

                RecordStatus.DUE_TODAY,
                RecordStatus.DUE_SOON -> MaterialTheme.colorScheme.tertiary

                RecordStatus.PAID,
                RecordStatus.ACTIVE -> MaterialTheme.colorScheme.primary
            }

            Text(
                text = stringResource(R.string.details_title),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = record.title,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(
                    if (isBill) R.string.record_type_bill
                    else R.string.record_type_document
                ),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = stringResource(statusLabel),
                style = MaterialTheme.typography.titleMedium,
                color = statusColor
            )

            record.issuer?.takeIf { it.isNotBlank() }?.let { issuer ->
                Text(stringResource(R.string.details_issuer, issuer))
            }
            record.issuedOn?.let { issuedOn ->
                Text(
                    stringResource(
                        R.string.details_issued_on,
                        issuedOn.format(detailsDateFormatter)
                    )
                )
            }
            Text(
                text = stringResource(
                    if (isBill) R.string.record_pay_until
                    else R.string.record_valid_until,
                    record.deadline.format(detailsDateFormatter)
                )
            )

            if (isBill) {
                val amountMinor = record.amountMinor
                val recordCurrency = record.currency

                if (amountMinor != null && recordCurrency != null) {
                    val formatter = NumberFormat.getCurrencyInstance().apply {
                        currency = Currency.getInstance(recordCurrency.name)
                    }
                    Text(
                        text = stringResource(
                            R.string.details_amount,
                            formatter.format(BigDecimal.valueOf(amountMinor, 2))
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Text(
                    text = stringResource(
                        if (record.isPaid) R.string.record_paid
                        else R.string.record_unpaid
                    )
                )
            }

            record.note?.takeIf { it.isNotBlank() }?.let { note ->
                Text(
                    text = stringResource(R.string.details_note),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(text = note)
            }
        }
    }
}
