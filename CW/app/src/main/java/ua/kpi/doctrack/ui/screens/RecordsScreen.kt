package ua.kpi.doctrack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Currency
import ua.kpi.doctrack.R
import ua.kpi.doctrack.model.DocumentRecord
import ua.kpi.doctrack.model.RecordType

private val recordDateFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yyyy")

@Composable
fun RecordsScreen(
    records: List<DocumentRecord>,
    modifier: Modifier = Modifier
) {
    if (records.isEmpty()) {
        Column(
            modifier = modifier
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
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.records_demo_notice),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            items(
                items = records,
                key = { record -> record.id }
            ) { record ->
                RecordCard(record = record)
            }
        }
    }
}

@Composable
private fun RecordCard(
    record: DocumentRecord,
    modifier: Modifier = Modifier
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

    Card(modifier = modifier.fillMaxWidth()) {
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
                    record.deadline.format(recordDateFormatter)
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

                Text(
                    text = stringResource(
                        if (record.isPaid) {
                            R.string.record_paid
                        } else {
                            R.string.record_unpaid
                        }
                    ),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}