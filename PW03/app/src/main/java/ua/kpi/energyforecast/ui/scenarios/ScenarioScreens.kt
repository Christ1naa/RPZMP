package ua.kpi.energyforecast.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import ua.kpi.energyforecast.model.ScenarioCategory
import ua.kpi.energyforecast.ui.scenarios.ScenarioSort
import ua.kpi.energyforecast.ui.scenarios.ScenarioUiState

@Composable
fun ScenarioCatalogScreen(
    state: ScenarioUiState,
    onQueryChange: (String) -> Unit,
    onCategoryChange: (ScenarioCategory?) -> Unit,
    onSortChange: (ScenarioSort) -> Unit,
    onRefresh: () -> Unit,
    onRetryCache: () -> Unit,
    onOpenScenario: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleItems = state.visibleItems

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 280.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Каталог сценаріїв",
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(
                    "Готові набори приладів для оцінки споживання " +
                            "та додавання до вашого профілю."
                )

                Button(
                    onClick = onRefresh,
                    enabled = !state.isRefreshing,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (state.isRefreshing) {
                            "Оновлення…"
                        } else {
                            "Оновити каталог"
                        }
                    )
                }

                if (state.isLoadingCache || state.isRefreshing) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                state.catalog.updatedAtMillis?.let { timestamp ->
                    Text(
                        "Збережена копія. Оновлено: ${formatCacheTime(timestamp)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                ScenarioMessages(state)

                if (state.cacheError != null) {
                    OutlinedButton(onClick = onRetryCache) {
                        Text("Повторити читання")
                    }
                }

                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    label = { Text("Пошук за назвою") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                CategorySelector(
                    selected = state.category,
                    onSelected = onCategoryChange
                )

                SortSelector(
                    selected = state.sort,
                    onSelected = onSortChange
                )

                Text(
                    "Показано: ${visibleItems.size} " +
                            "із ${state.catalog.scenarios.size}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        if (!state.isLoadingCache && visibleItems.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = when {
                            !state.hasCachedCatalog ->
                                "Каталог ще не завантажено."

                            state.catalog.scenarios.isEmpty() ->
                                "Завантажений каталог порожній."

                            else ->
                                "За вибраними умовами сценаріїв не знайдено."
                        },
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        items(
            items = visibleItems,
            key = { it.scenario.id }
        ) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        item.scenario.name,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(item.scenario.category.displayName())
                    Text(item.scenario.description)

                    Text("Позицій: ${item.scenario.devices.size}")

                    Text(
                        "${formatScenarioEnergy(item.dailyConsumptionKwh)} " +
                                "кВт·год/добу"
                    )

                    OutlinedButton(
                        onClick = { onOpenScenario(item.scenario.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Деталі та прогноз")
                    }
                }
            }
        }
    }
}

@Composable
fun ScenarioDetailsScreen(
    state: ScenarioUiState,
    onDaysChange: (String) -> Unit,
    onImport: (String) -> Unit,
    onOpenAppliances: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scenario = state.selectedScenario
    val forecast = state.selectedForecast

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (scenario == null) {
            item {
                Text(
                    if (state.isLoadingCache) {
                        "Завантаження…"
                    } else {
                        "Сценарій відсутній у збереженому каталозі."
                    }
                )
            }

            item { ScenarioMessages(state) }
        } else {
            item {
                Text(
                    scenario.name,
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            item {
                Text(scenario.category.displayName())
                Text(scenario.description)
            }

            item {
                OutlinedTextField(
                    value = state.daysText,
                    onValueChange = onDaysChange,
                    label = { Text("Період прогнозу, днів") },
                    singleLine = true,
                    isError = state.forecastDays == null,
                    supportingText = {
                        Text("Ціле число від 1 до 365")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (forecast != null) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Прогноз споживання",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                "За добу: " +
                                        formatScenarioEnergy(
                                            forecast.dailyConsumptionKwh
                                        ) +
                                        " кВт·год"
                            )

                            Text(
                                "За ${forecast.forecastDays} днів: " +
                                        formatScenarioEnergy(
                                            forecast.periodConsumptionKwh
                                        ) +
                                        " кВт·год"
                            )
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { onImport(scenario.id) },
                    enabled = !state.isImporting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (state.isImporting) {
                            "Додавання…"
                        } else {
                            "Додати прилади до мого профілю"
                        }
                    )
                }

                Text(
                    "Імпорт додає всі позиції. Наявні прилади та період " +
                            "вашого профілю зберігаються.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            item {
                ScenarioMessages(state)

                OutlinedButton(
                    onClick = onOpenAppliances,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Перейти до моїх приладів")
                }
            }

            item {
                Text(
                    "Прилади сценарію",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            items(
                items = scenario.devices,
                key = { it.id }
            ) { device ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            device.name,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            "${formatScenarioEnergy(device.powerWatts)} Вт · " +
                                    "${formatScenarioEnergy(device.hoursPerDay)} " +
                                    "год/добу · ${device.deviceCount} шт."
                        )
                    }
                }
            }

            item {
                Text(
                    "Оцінка передбачає сталу потужність і однаковий " +
                            "режим роботи щодня.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ScenarioMessages(state: ScenarioUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.cacheError?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        state.refreshError?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        state.message?.let {
            Text(it)
        }
    }
}

@Composable
private fun CategorySelector(
    selected: ScenarioCategory?,
    onSelected: (ScenarioCategory?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Категорія: ${selected?.displayName() ?: "Усі"}")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Усі категорії") },
                onClick = {
                    expanded = false
                    onSelected(null)
                }
            )

            ScenarioCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.displayName()) },
                    onClick = {
                        expanded = false
                        onSelected(category)
                    }
                )
            }
        }
    }
}

@Composable
private fun SortSelector(
    selected: ScenarioSort,
    onSelected: (ScenarioSort) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selected.displayName())
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ScenarioSort.entries.forEach { sort ->
                DropdownMenuItem(
                    text = { Text(sort.displayName()) },
                    onClick = {
                        expanded = false
                        onSelected(sort)
                    }
                )
            }
        }
    }
}

private fun ScenarioCategory.displayName(): String = when (this) {
    ScenarioCategory.HOME -> "Дім"
    ScenarioCategory.OFFICE -> "Офіс"
    ScenarioCategory.MIXED -> "Змішані"
}

private fun ScenarioSort.displayName(): String = when (this) {
    ScenarioSort.NAME_ASC -> "За назвою: А–Я"
    ScenarioSort.DAILY_ASC -> "Споживання: від меншого"
    ScenarioSort.DAILY_DESC -> "Споживання: від більшого"
}

private fun formatScenarioEnergy(value: Double): String {
    return NumberFormat.getNumberInstance(
        Locale.forLanguageTag("uk-UA")
    ).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(value)
}

private fun formatCacheTime(timestamp: Long): String {
    return DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(timestamp))
}