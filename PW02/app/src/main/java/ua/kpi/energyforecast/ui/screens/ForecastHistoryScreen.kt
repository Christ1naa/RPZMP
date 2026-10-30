package ua.kpi.energyforecast.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import ua.kpi.energyforecast.model.SavedForecast
import ua.kpi.energyforecast.ui.components.ConsumptionChart
import ua.kpi.energyforecast.ui.history.ForecastHistoryUiState
import ua.kpi.energyforecast.ui.history.HistoryPeriodFilter
import ua.kpi.energyforecast.ui.history.HistorySort

/** Показує історію, пошук, фільтр періоду та порядок записів. */
@Composable
fun ForecastHistoryScreen(
    state: ForecastHistoryUiState,
    onSearchChange: (String) -> Unit,
    onFilterChange: (HistoryPeriodFilter) -> Unit,
    onSortChange: (HistorySort) -> Unit,
    onOpenForecast: (String) -> Unit,
    onDeleteForecast: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }

    val deletingForecast = state.forecasts.firstOrNull {
        it.id == deletingId
    }
    val visibleForecasts = state.visibleForecasts

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Історія прогнозів",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchChange,
                    label = { Text("Назва прогнозу або приладу") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            TextButton(onClick = { onSearchChange("") }) {
                                Text("Очистити")
                            }
                        }
                    }
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HistoryOptionMenu(
                        label = "Період",
                        selected = state.periodFilter.title,
                        options = HistoryPeriodFilter.entries.map { it.title },
                        onSelected = { index ->
                            onFilterChange(HistoryPeriodFilter.entries[index])
                        }
                    )

                    HistoryOptionMenu(
                        label = "Сортування",
                        selected = state.sort.title,
                        options = HistorySort.entries.map { it.title },
                        onSelected = { index ->
                            onSortChange(HistorySort.entries[index])
                        }
                    )

                    Text(
                        text = "Показано: ${visibleForecasts.size} " +
                                "із ${state.forecasts.size}"
                    )
                }
            }

            if (state.isLoading) {
                item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            state.loadError?.let { error ->
                item {
                    Column {
                        Text(error, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRetry) {
                            Text("Повторити завантаження")
                        }
                    }
                }
            }

            state.operationError?.let { error ->
                item {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }

            state.message?.let { message ->
                item {
                    Text(message, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (!state.isLoading && state.loadError == null) {
                if (state.forecasts.isEmpty()) {
                    item {
                        Text(
                            "Історія порожня. Збережіть прогноз " +
                                    "на екрані «Мої прилади»."
                        )
                    }
                } else if (visibleForecasts.isEmpty()) {
                    item {
                        Text("За вибраними умовами прогнозів не знайдено.")
                    }
                }
            }

            items(
                items = visibleForecasts,
                key = { it.id }
            ) { forecast ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = forecast.title,
                            style = MaterialTheme.typography.titleLarge
                        )

                        Text(formatForecastTime(forecast.createdAt))

                        Text(
                            text = "${forecast.result.forecastDays} днів · " +
                                    "${forecast.devices.size} позицій"
                        )

                        Text(
                            text = "${formatEnergy(
                                forecast.result.periodConsumptionKwh
                            )} кВт·год за період"
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextButton(
                                onClick = { onOpenForecast(forecast.id) }
                            ) {
                                Text("Деталі")
                            }

                            TextButton(
                                enabled = !state.isDeleting,
                                onClick = { deletingId = forecast.id }
                            ) {
                                Text(
                                    text = "Видалити",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (deletingForecast != null) {
        AlertDialog(
            onDismissRequest = { deletingId = null },
            title = { Text("Видалити прогноз?") },
            text = {
                Text(
                    "Запис «${deletingForecast.title}» " +
                            "і його збережені позиції буде видалено."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !state.isDeleting,
                    onClick = {
                        onDeleteForecast(deletingForecast.id)
                        deletingId = null
                    }
                ) {
                    Text("Видалити")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingId = null }) {
                    Text("Скасувати")
                }
            }
        )
    }
}

/** Показує збережений результат і незмінні копії параметрів приладів. */
@Composable
fun ForecastDetailsScreen(
    forecast: SavedForecast?,
    isLoading: Boolean,
    loadError: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 840.dp)
                .fillMaxWidth()
                .fillMaxHeight(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Деталі прогнозу",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            when {
                isLoading -> item {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                loadError != null -> item {
                    Column {
                        Text(
                            text = loadError,
                            color = MaterialTheme.colorScheme.error
                        )
                        TextButton(onClick = onRetry) {
                            Text("Повторити завантаження")
                        }
                    }
                }

                forecast == null -> item {
                    Text("Прогноз не знайдено або його вже видалено.")
                }

                else -> {
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = forecast.title,
                                style = MaterialTheme.typography.titleLarge
                            )

                            Text(formatForecastTime(forecast.createdAt))

                            Text("Період: ${forecast.result.forecastDays} днів")

                            Text(
                                "На добу: ${
                                    formatEnergy(
                                        forecast.result.dailyConsumptionKwh
                                    )
                                } кВт·год"
                            )

                            Text(
                                "За період: ${
                                    formatEnergy(
                                        forecast.result.periodConsumptionKwh
                                    )
                                } кВт·год"
                            )

                            Text(
                                text = "Нижче показані параметри на момент " +
                                        "збереження прогнозу.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    items(
                        items = forecast.devices,
                        key = { it.id }
                    ) { device ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = device.name,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Text(
                                    "Потужність одного: " +
                                            "${formatEnergy(device.powerWatts)} Вт"
                                )

                                Text(
                                    "Робота на добу: " +
                                            "${formatEnergy(device.hoursPerDay)} год"
                                )

                                Text("Кількість: ${device.deviceCount}")
                            }
                        }
                    }

                    item {
                        ConsumptionChart(result = forecast.result)
                    }
                }
            }
        }
    }
}

/** Дає вибрати одну умову без розширення екрана за його межі. */
@Composable
private fun HistoryOptionMenu(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("$label: $selected")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEachIndexed { index, title ->
                DropdownMenuItem(
                    text = { Text(title) },
                    onClick = {
                        onSelected(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun formatEnergy(value: Double): String =
    String.format(Locale.forLanguageTag("uk-UA"), "%.2f", value)

private fun formatForecastTime(value: Long): String =
    Instant.ofEpochMilli(value)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))