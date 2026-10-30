package ua.kpi.energyforecast.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ua.kpi.energyforecast.R
import ua.kpi.energyforecast.model.EnergyDevice
import ua.kpi.energyforecast.ui.components.ConsumptionChart
import ua.kpi.energyforecast.ui.components.DeviceCard
import ua.kpi.energyforecast.ui.components.DeviceEditorDialog
import ua.kpi.energyforecast.ui.forecast.ForecastUiState
import ua.kpi.energyforecast.ui.forecast.DeviceSort
import ua.kpi.energyforecast.ui.components.DemoScenariosCard

/**
 * Відображає стан прогнозу й передає дії користувача назовні.
 * Локально зберігає тільки відкриття діалогів та id вибраних позицій.
 */
@Composable
fun ApplianceForecastScreen(
    state: ForecastUiState,
    onDaysChange: (String) -> Unit,
    onSaveDevice: (EnergyDevice) -> Unit,
    onDeleteDevice: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSortChange: (DeviceSort) -> Unit,
    onRetryLoad: () -> Unit,
    onLoadDemoScenarios: (Boolean) -> Unit,
    onImportDemoScenario: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val devices = state.devices
    val visibleDevices = state.visibleDevices

    var sortMenuExpanded by remember {
        mutableStateOf(false)
    }
    val forecastDays = state.forecastDays
    val result = state.result

    var editorOpen by rememberSaveable {
        mutableStateOf(false)
    }

    var editingId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var deletingId by rememberSaveable {
        mutableStateOf<String?>(null)
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
                    value = state.daysText,
                    onValueChange = onDaysChange,
                    label = {
                        Text("Період прогнозування, днів")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = forecastDays == null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    supportingText = {
                        Text(
                            text = if (forecastDays == null) {
                                "Введіть ціле число від 1 до 365"
                            } else {
                                "Прогноз оновлюється автоматично"
                            }
                        )
                    }
                )
            }

            if (state.isLoading) {
                item(key = "loading") {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            state.loadError?.let { message ->
                item(key = "load_error") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error
                        )

                        OutlinedButton(
                            onClick = onRetryLoad,
                            enabled = !state.isLoading
                        ) {
                            Text("Повторити завантаження")
                        }
                    }
                }
            }

            state.error?.let { message ->
                item(key = "operation_error") {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            item(key = "summary") {
                Text(
                    text = "Позицій: ${devices.size}. " +
                            "Усього приладів: ${state.totalDeviceCount}",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (result != null) {
                item(key = "result") {
                    ForecastResultCard(result = result)
                }
            }

            item(key = "add_button") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            editingId = null
                            editorOpen = true
                        },
                        enabled = state.canAddDevice,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Додати прилад")
                    }

                    if (!state.canAddDevice) {
                        Text(
                            text = "Досягнуто межі: " +
                                    "${ForecastUiState.MAX_DEVICE_POSITIONS} позицій.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item(key = "demo_scenarios") {
                DemoScenariosCard(
                    state = state,
                    onLoad = onLoadDemoScenarios,
                    onImport = onImportDemoScenario
                )
            }

            item(key = "search_and_sort") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Пошук за назвою")
                        },
                        singleLine = true,
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        onSearchQueryChange("")
                                    }
                                ) {
                                    Text("Очистити")
                                }
                            }
                        }
                    )

                    Box {
                        OutlinedButton(
                            onClick = {
                                sortMenuExpanded = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(state.sort.title)
                        }

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = {
                                sortMenuExpanded = false
                            }
                        ) {
                            DeviceSort.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(option.title)
                                    },
                                    onClick = {
                                        onSortChange(option)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Text(
                        text = "Показано позицій: ${visibleDevices.size} " +
                                "із ${devices.size}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "Прогноз і графік враховують усі прилади профілю.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (devices.isEmpty()) {
                item(key = "empty_state") {
                    EmptyDeviceList()
                }
            } else if (visibleDevices.isEmpty()) {
                item(key = "no_search_results") {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "За цим запитом приладів не знайдено",
                            style = MaterialTheme.typography.titleMedium
                        )

                        TextButton(
                            onClick = {
                                onSearchQueryChange("")
                            }
                        ) {
                            Text("Очистити пошук")
                        }
                    }
                }
            }

            items(
                items = visibleDevices,
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                // Список і результат змінює ViewModel; екран закриває лише діалог.
                onSaveDevice(savedDevice)
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
                    text = "Позицію «${deletingDevice.name}» " +
                            "буде вилучено зі списку."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteDevice(deletingDevice.id)
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

/** Пояснює призначення списку приладів і позначає предметну область зображенням. */
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

/** Підказує першу дію, коли список порожній і розрахунок ще не показується. */
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