package ua.kpi.energyforecast.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ua.kpi.energyforecast.logic.DeviceValidator
import ua.kpi.energyforecast.logic.ForecastValidator
import ua.kpi.energyforecast.model.EnergyDevice
import java.util.UUID

/**
 * Редагує окрему чернетку: список змінюється лише через onSave після валідації.
 * Скасування відкидає чернетку; редагування зберігає id, нова позиція отримує UUID.
 */
@Composable
fun DeviceEditorDialog(
    device: EnergyDevice?,
    onSave: (EnergyDevice) -> Unit,
    onDismiss: () -> Unit
) {
    // Чернетка переживає поворот; зміна id під час роботи компонента скидає її для іншого приладу.
    var nameText by rememberSaveable(device?.id) {
        mutableStateOf(device?.name ?: "")
    }

    var powerText by rememberSaveable(device?.id) {
        mutableStateOf(device?.powerWatts?.toString() ?: "")
    }

    var hoursText by rememberSaveable(device?.id) {
        mutableStateOf(device?.hoursPerDay?.toString() ?: "")
    }

    var countText by rememberSaveable(device?.id) {
        mutableStateOf(device?.deviceCount?.toString() ?: "1")
    }

    var showErrors by rememberSaveable(device?.id) {
        mutableStateOf(false)
    }

    val validation = DeviceValidator.validate(
        nameText = nameText,
        powerText = powerText,
        hoursText = hoursText,
        countText = countText
    )

    val errors = if (showErrors) {
        validation.errors
    } else {
        emptyMap()
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text(
                if (device == null) {
                    "Додати прилад"
                } else {
                    "Редагувати прилад"
                }
            )
        },

        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DeviceInputField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = "Назва приладу",
                    error = errors[DeviceValidator.NAME],
                    keyboardType = KeyboardType.Text
                )

                DeviceInputField(
                    value = powerText,
                    onValueChange = { powerText = it },
                    label = "Потужність одного приладу, Вт",
                    error = errors[ForecastValidator.POWER],
                    keyboardType = KeyboardType.Decimal
                )

                DeviceInputField(
                    value = hoursText,
                    onValueChange = { hoursText = it },
                    label = "Час роботи на добу, год",
                    error = errors[ForecastValidator.HOURS],
                    keyboardType = KeyboardType.Decimal
                )

                DeviceInputField(
                    value = countText,
                    onValueChange = { countText = it },
                    label = "Кількість однакових приладів",
                    error = errors[ForecastValidator.COUNT],
                    keyboardType = KeyboardType.Number
                )
            }
        },

        confirmButton = {
            TextButton(
                onClick = {
                    showErrors = true

                    val parameters = validation.parameters

                    if (parameters != null) {
                        onSave(
                            EnergyDevice(
                                id = device?.id
                                    ?: UUID.randomUUID().toString(),
                                name = validation.name,
                                powerWatts = parameters.powerWatts,
                                hoursPerDay = parameters.hoursPerDay,
                                deviceCount = parameters.deviceCount
                            )
                        )
                    }
                }
            ) {
                Text("Зберегти")
            }
        },

        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Скасувати")
            }
        }
    )
}

/**
 * Узгоджує підпис, клавіатуру й повідомлення про помилку для полів редактора.
 */
@Composable
private fun DeviceInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    keyboardType: KeyboardType
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = error != null,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType
        ),
        supportingText = {
            if (error != null) {
                Text(error)
            }
        }
    )
}