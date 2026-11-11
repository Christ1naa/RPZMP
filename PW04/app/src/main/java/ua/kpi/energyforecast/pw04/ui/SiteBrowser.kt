package ua.kpi.energyforecast.pw04.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import ua.kpi.energyforecast.pw04.logic.*
import ua.kpi.energyforecast.pw04.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteBrowser(
    state: ForecastUiState,
    onQuery: (SiteQuery) -> Unit,
    onSelect: (String) -> Unit,
    onDetails: (String) -> Unit,
    map: @Composable () -> Unit
) {
    var showFilters by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val ids = state.visible.map { it.site.id }
    // Маркер обирає спільний ID; список прокручується саме до нього навіть після сортування.
    LaunchedEffect(state.selectedId, ids) {
        val index = ids.indexOf(state.selectedId)
        if (index >= 0) listState.animateScrollToItem(index)
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Об'єкти споживання", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            TextButton(onClick = { showFilters = true }) { Text("Фільтри") }
        }
        OutlinedTextField(value = state.query.search, onValueChange = { onQuery(state.query.copy(search = it)) },
            label = { Text("Пошук за назвою") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("Знайдено: ${state.visible.size} • ${state.query.primary.label}, ${state.query.secondary.label}", style = MaterialTheme.typography.labelMedium)
        state.query.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Box(Modifier.fillMaxWidth().height(230.dp)) { map() }
        if (state.visible.isEmpty()) {
            Text(if (state.telemetry.phase == ua.kpi.energyforecast.pw04.data.StreamPhase.WAITING) "Очікування даних…" else "За цими умовами об'єктів немає.")
            TextButton(onClick = { onQuery(SiteQuery()) }) { Text("Скинути фільтри") }
        }
        LazyColumn(Modifier.weight(1f).testTag("site-list"), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.visible, key = { it.site.id }) { reading ->
                val site = reading.site
                val isSelected = site.id == state.selectedId
                Card(onClick = { onSelect(site.id) },
                    modifier = Modifier.fillMaxWidth().testTag("site-${site.id}").semantics { selected = isSelected },
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(site.name, style = MaterialTheme.typography.titleMedium)
                        Text("${site.type.label} • ${site.status.label}")
                        Text(site.description)
                        Text("${number(site.ratedPowerKw)} кВт • ${number(reading.forecastDailyKwh)} кВт·год/добу")
                        Text("Оновлено: ${site.updatedAt}", style = MaterialTheme.typography.labelSmall)
                        TextButton(onClick = { onSelect(site.id); onDetails(site.id) }) { Text("Деталі") }
                    }
                }
            }
        }
    }
    if (showFilters) ModalBottomSheet(onDismissRequest = { showFilters = false }) {
        LazyColumn(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Фільтрація та порядок", style = MaterialTheme.typography.titleLarge) }
            item { Choice("Тип", listOf<SiteType?>(null) + SiteType.entries, state.query.type, { it?.label ?: "Усі" }) { onQuery(state.query.copy(type = it)) } }
            item { Choice("Статус", listOf<SiteStatus?>(null) + SiteStatus.entries, state.query.status, { it?.label ?: "Усі" }) { onQuery(state.query.copy(status = it)) } }
            item { OutlinedTextField(state.query.minPowerKw, { onQuery(state.query.copy(minPowerKw = it)) }, label = { Text("Мін. встановлена потужність, кВт") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(state.query.maxPowerKw, { onQuery(state.query.copy(maxPowerKw = it)) }, label = { Text("Макс. встановлена потужність, кВт") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
            item { state.query.error?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
            item { Choice("Перший критерій", SiteSort.entries, state.query.primary, { it.label }) { onQuery(state.query.copy(primary = it)) } }
            item { Choice("Другий критерій", SiteSort.entries, state.query.secondary, { it.label }) { onQuery(state.query.copy(secondary = it)) } }
            item { Row { Checkbox(state.query.descending, { onQuery(state.query.copy(descending = it)) }); Text("За спаданням обох критеріїв") } }
            item { Row {
                TextButton(onClick = { onQuery(SiteQuery()) }) { Text("Скинути") }
                Button(onClick = { showFilters = false }) { Text("Готово") }
            } }
        }
    }
}

@Composable
fun <T> Choice(label: String, options: List<T>, value: T, text: (T) -> String, onChange: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text("$label: ${text(value)}") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(text(option)) }, onClick = { onChange(option); expanded = false }) }
        }
    }
}
