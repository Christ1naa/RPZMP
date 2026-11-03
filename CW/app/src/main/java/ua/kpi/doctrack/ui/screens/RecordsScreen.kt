package ua.kpi.doctrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import ua.kpi.doctrack.model.RecordDates
import ua.kpi.doctrack.ui.RecordStatusText
import ua.kpi.doctrack.R
import ua.kpi.doctrack.model.DocumentRecord
import ua.kpi.doctrack.model.RecordType
import java.time.LocalDate


@Composable
fun RecordsScreen(
    records: List<DocumentRecord>,
    today: LocalDate,
    onRecordClick: (DocumentRecord) -> Unit,
    onAddRecord: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        Button(
            onClick = onAddRecord,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) { Text(stringResource(R.string.form_add_action)) }
        Text(
            text = stringResource(R.string.records_memory_notice),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodySmall
        )
        Box(modifier = Modifier.weight(1f)) {
            if (records.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.records_empty_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = stringResource(R.string.records_empty_description),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.records_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    items(
                        items = records,
                        key = { record -> record.id }
                    ) { record ->
                        RecordCard(
                            record = record,
                            onClick = { onRecordClick(record) },
                            today = today
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun RecordCard(
    record: DocumentRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    today: LocalDate
) {
    val isBill = record.type == RecordType.BILL

    val typeLabel = if (isBill) {
        R.string.record_type_bill
    } else {
        R.string.record_type_document
    }

    val deadlineLabel = if (isBill) {
        R.string.record_pay_until
    } else {
        R.string.record_valid_until
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(typeLabel),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = record.title,
                style = MaterialTheme.typography.titleMedium
            )

            record.issuer?.let { issuer ->
                Text(
                    text = issuer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = stringResource(
                    deadlineLabel,
                    record.deadline.format(RecordDates.formatter)
                ),
                style = MaterialTheme.typography.bodyMedium
            )

            if (isBill) {
                val amountMinor = record.amountMinor
                val currency = record.currency

                if (amountMinor != null && currency != null) {
                    val amountFormatter =
                        NumberFormat.getCurrencyInstance().apply {
                            this.currency = Currency.getInstance(currency.name)
                        }

                    Text(
                        text = amountFormatter.format(
                            BigDecimal.valueOf(amountMinor, 2)
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

            }

            RecordStatusText(record = record, today = today)

        }
    }
}

