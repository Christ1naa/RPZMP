package ua.kpi.energyforecast.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ua.kpi.energyforecast.R
import ua.kpi.energyforecast.logic.DeviceValidator
import ua.kpi.energyforecast.logic.EnergyCalculator
import ua.kpi.energyforecast.model.EnergyDevice
import ua.kpi.energyforecast.ui.components.ConsumptionChart
import ua.kpi.energyforecast.ui.components.DeviceCard
import ua.kpi.energyforecast.ui.components.DeviceEditorDialog

private const val MAX_DEVICE_POSITIONS = 50

/**
 * Перетворює список на підтримувані saved state значення: по п’ять полів на прилад.
 * Відновлення читає той самий порядок і зберігає id для редагування та ключів LazyColumn.
 * Це механізм відновлення UI, а не постійне сховище після повного закриття застосунку.
 */
private val DeviceListSaver = listSaver<List<EnergyDevice>, Any>(
    save = { devices ->
        devices.flatMap { device ->
            listOf<Any>(
                device.id,
                device.name,
                device.powerWatts,
                device.hoursPerDay,
                device.deviceCount
            )
        }
    },
    restore = { savedValues ->
        savedValues.chunked(5).map { values ->
            EnergyDevice(
                id = values[0] as String,
                name = values[1] as String,
                powerWatts = (values[2] as Number).toDouble(),
                hoursPerDay = (values[3] as Number).toDouble(),
                deviceCount = (values[4] as Number).toInt()
            )
        }
    }
)

/**
 * Керує списком приладів, періодом і діалогами в межах стану поточного користувача.
 * Зміни списку або валідного періоду автоматично оновлюють результат і графік.
 */
@Composable
fun ApplianceForecastScreen(
    modifier: Modifier = Modifier
) {
    var devices by rememberSaveable(
        stateSaver = DeviceListSaver
    ) {
        mutableStateOf(emptyList<EnergyDevice>())
    }

    var daysText by rememberSaveable {
        mutableStateOf("30")
    }

    var editorOpen by rememberSaveable {
        mutableStateOf(false)
    }

    var editingId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var deletingId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val forecastDays = DeviceValidator.parseForecastDays(daysText)

    // Результат похідний: окремо його не зберігаємо, щоб він не розходився з параметрами.
    // Порожній список або невалідний період приховують результат замість показу застарілих даних.
    val result = remember(devices, forecastDays) {
        if (devices.isNotEmpty() && forecastDays != null) {
            EnergyCalculator.calculateTotal(
                devices = devices,
                forecastDays = forecastDays
            )
        } else {
            null
        }
    }

    val editingDevice = devices.firstOrNull {
        it.id == editingId
    }

    val deletingDevice = devices.firstOrNull {
        it.id == deletingId
    }

    Box(
        modifier = modifier.imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxWidth()
                .fillMaxHeight(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "header") {
                ApplianceHeader()
            }

            item(key = "period") {
                OutlinedTextField(
                    value = daysText,
                    onValueChange = { daysText = it },
                    label = { Text("Період прогнозування, днів") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = forecastDays == null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    supportingText = {
                        Text(
                            if (forecastDays == null) {
                                "Введіть ціле число від 1 до 365"
                            } else {
                                "Прогноз оновлюється автоматично"
                            }
                        )
                    }
                )
            }

            item(key = "summary") {
                Text(
                    text = "Позицій: ${devices.size}. " +
                            "Усього приладів: " +
                            devices.sumOf { it.deviceCount },
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (result != null) {
                item(key = "result") {
                    ForecastResultCard(result = result)
                }
            }

            item(key = "add_button") {
                Button(
                    onClick = {
                        editingId = null
                        editorOpen = true
                    },
                    enabled = devices.size < MAX_DEVICE_POSITIONS,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Додати прилад")
                }

                if (devices.size >= MAX_DEVICE_POSITIONS) {
                    Text(
                        text = "Досягнуто межі: " +
                                "$MAX_DEVICE_POSITIONS позицій.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (devices.isEmpty()) {
                item(key = "empty_state") {
                    EmptyDeviceList()
                }
            }

            items(
                items = devices,
                key = { device -> "device_${device.id}" }
            ) { device ->
                DeviceCard(
                    device = device,
                    onEdit = {
                        editingId = device.id
                        editorOpen = true
                    },
                    onDelete = {
                        deletingId = device.id
                    }
                )
            }

            if (result != null) {
                item(key = "chart") {
                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Загальне накопичене споживання",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Text(
                            text = "Вісь X — дні. " +
                                    "Вісь Y — енергія, кВт·год.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        ConsumptionChart(result = result)
                    }
                }
            }

            item(key = "assumption") {
                Text(
                    text = "Прогноз передбачає сталу потужність " +
                            "та однаковий час роботи приладів щодня.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (editorOpen) {
        DeviceEditorDialog(
            device = editingDevice,
            onSave = { savedDevice ->
                devices = if (editingId == null) {
                    devices + savedDevice
                } else {
                    devices.map { current ->
                        if (current.id == savedDevice.id) {
                            savedDevice
                        } else {
                            current
                        }
                    }
                }

                editorOpen = false
                editingId = null
            },
            onDismiss = {
                editorOpen = false
                editingId = null
            }
        )
    }

    if (deletingDevice != null) {
        AlertDialog(
            onDismissRequest = {
                deletingId = null
            },
            title = {
                Text("Видалити прилад?")
            },
            text = {
                Text(
                    "Позицію «${deletingDevice.name}» " +
                            "буде вилучено зі списку."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        devices = devices.filterNot {
                            it.id == deletingDevice.id
                        }
                        deletingId = null
                    }
                ) {
                    Text(
                        text = "Видалити",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deletingId = null
                    }
                ) {
                    Text("Скасувати")
                }
            }
        )
    }
}

/**
 * Пояснює призначення списку приладів і позначає предметну область зображенням.
 */
@Composable
private fun ApplianceHeader() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = painterResource(
                id = R.drawable.energy_illustration
            ),
            contentDescription = "Символ електроенергії",
            modifier = Modifier.size(80.dp)
        )

        Text(
            text = "Мої електроприлади",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Додайте різні прилади, щоб отримати " +
                    "загальний прогноз споживання.",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

/**
 * Підказує першу дію, коли список порожній і розрахунок ще не показується.
 */
@Composable
private fun EmptyDeviceList() {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Приладів поки немає",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Натисніть «Додати прилад» і введіть " +
                        "його потужність та режим роботи.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}