package ua.kpi.doctrack.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.UUID
import ua.kpi.doctrack.R
import ua.kpi.doctrack.model.DocumentRecord
import ua.kpi.doctrack.model.RecordCurrency
import ua.kpi.doctrack.model.RecordType
import ua.kpi.doctrack.ui.form.RecordFormError
import ua.kpi.doctrack.ui.form.RecordFormField
import ua.kpi.doctrack.ui.form.RecordFormInput
import ua.kpi.doctrack.ui.form.validateRecordForm

private val formInputSaver = listSaver<RecordFormInput, String>(
    save = {
        listOf(it.title, it.type.name, it.issuer, it.issuedOn, it.deadline,
            it.amount, it.currency.name, it.isPaid.toString(), it.note)
    },
    restore = {
        RecordFormInput(it[0], RecordType.valueOf(it[1]), it[2], it[3], it[4],
            it[5], RecordCurrency.valueOf(it[6]), it[7].toBoolean(), it[8])
    }
)

@Composable
fun RecordFormScreen(
    record: DocumentRecord?,
    onSave: (DocumentRecord) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initial = remember(record?.id) { RecordFormInput.from(record) }
    var input by rememberSaveable(record?.id, stateSaver = formInputSaver) {
        mutableStateOf(initial)
    }
    val newId = rememberSaveable { UUID.randomUUID().toString() }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val result = remember(input, record, newId) {
        validateRecordForm(input, record, newId)
    }
    val errors = if (submitted) result.errors else emptyMap()
    val requestCancel: () -> Unit = {
        if (input != initial) confirmDiscard = true else onCancel()
    }

    BackHandler(onBack = requestCancel)

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.form_discard_title)) },
            text = { Text(stringResource(R.string.form_discard_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onCancel()
                }) { Text(stringResource(R.string.form_discard)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) {
                    Text(stringResource(R.string.form_keep_editing))
                }
            }
        )
    }

    Column(
        modifier = modifier.fillMaxSize().imePadding()
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = requestCancel) {
            Text(stringResource(R.string.form_cancel))
        }
        Text(
            text = stringResource(
                if (record == null) R.string.form_create_title else R.string.form_edit_title
            ),
            style = MaterialTheme.typography.headlineSmall
        )
        Text(stringResource(R.string.records_memory_notice))
        Text(stringResource(R.string.form_type), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RecordType.entries.forEach { type ->
                FilterChip(
                    selected = input.type == type,
                    onClick = { input = input.copy(type = type) },
                    label = {
                        Text(stringResource(
                            if (type == RecordType.BILL) R.string.record_type_bill
                            else R.string.record_type_document
                        ))
                    }
                )
            }
        }
        FormTextField(input.title, { input = input.copy(title = it) },
            R.string.form_title, errors[RecordFormField.TITLE])
        FormTextField(input.issuer, { input = input.copy(issuer = it) },
            R.string.form_issuer)
        FormTextField(input.issuedOn, { input = input.copy(issuedOn = it) },
            R.string.form_issued_on, errors[RecordFormField.ISSUED_ON])
        FormTextField(input.deadline, { input = input.copy(deadline = it) },
            if (input.type == RecordType.BILL) R.string.form_pay_until
            else R.string.form_valid_until, errors[RecordFormField.DEADLINE])

        if (input.type == RecordType.BILL) {
            FormTextField(input.amount, { input = input.copy(amount = it) },
                R.string.form_amount, errors[RecordFormField.AMOUNT],
                keyboardType = KeyboardType.Decimal)
            Text(stringResource(R.string.form_currency),
                style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RecordCurrency.entries.forEach { currency ->
                    FilterChip(
                        selected = input.currency == currency,
                        onClick = { input = input.copy(currency = currency) },
                        label = { Text(currency.name) }
                    )
                }
            }
            Row {
                Checkbox(
                    checked = input.isPaid,
                    onCheckedChange = { input = input.copy(isPaid = it) }
                )
                TextButton(onClick = { input = input.copy(isPaid = !input.isPaid) }) {
                    Text(stringResource(R.string.record_paid))
                }
            }
        }
        FormTextField(input.note, { input = input.copy(note = it) },
            R.string.form_note, singleLine = false)
        errors[RecordFormField.TYPE]?.let {
            Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = {
                submitted = true
                result.record?.let(onSave)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.form_save)) }
    }
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes labelRes: Int,
    error: RecordFormError? = null,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(labelRes)) },
        isError = error != null,
        supportingText = {
            if (error != null) Text(stringResource(error.messageRes()))
        },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth()
    )
}

@StringRes
private fun RecordFormError.messageRes(): Int = when (this) {
    RecordFormError.REQUIRED -> R.string.form_error_required
    RecordFormError.TITLE_TOO_LONG -> R.string.form_error_title_length
    RecordFormError.INVALID_DATE -> R.string.form_error_date
    RecordFormError.DEADLINE_BEFORE_ISSUE_DATE -> R.string.form_error_date_order
    RecordFormError.INVALID_AMOUNT -> R.string.form_error_amount
    RecordFormError.AMOUNT_MUST_BE_POSITIVE -> R.string.form_error_positive_amount
    RecordFormError.DOCUMENT_HAS_BILL_FIELDS -> R.string.form_error_document_fields
}
