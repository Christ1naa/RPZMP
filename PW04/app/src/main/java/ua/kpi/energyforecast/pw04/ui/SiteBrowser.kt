package ua.kpi.energyforecast.pw04.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import ua.kpi.energyforecast.pw04.data.StreamPhase
import ua.kpi.energyforecast.pw04.logic.*
import ua.kpi.energyforecast.pw04.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiteBrowser(
    state: ForecastUiState,
    onQuery: (SiteQuery) -> Unit,
    onSelect: (String) -> Unit,
    onDetails: (String) -> Unit,
    onRestart: () -> Unit = {},
    map: @Composable () -> Unit
) {
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var showMap by rememberSaveable { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val ids = state.visible.map { it.site.id }
    val showStreamStatus = state.telemetry.phase != StreamPhase.LIVE
    val focus = LocalFocusManager.current
    // Маркер обирає спільний ID; службовий стан займає один елемент перед картками.
    LaunchedEffect(state.selectedId, state.selectionRevision, ids, showStreamStatus) {
        val index = ids.indexOf(state.selectedId)
        if (index >= 0) listState.animateScrollToItem(index + if (showStreamStatus) 1 else 0)
    }
    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        // У широкому/альбомному вікні мапа не забирає вертикальний простір списку.
        val sideBySide = maxWidth >= 600.dp || maxWidth > maxHeight
        val mapHeight = minOf(230.dp, maxHeight * 0.32f)
        Column(Modifier.fillMaxSize()) {
            Text("Об'єкти споживання", style = MaterialTheme.typography.titleLarge)
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { showMap = !showMap }, modifier = Modifier.testTag("map-toggle").semantics {
                    stateDescription = if (showMap) "Мапу показано" else "Мапу приховано"
                }) { Text(if (showMap) "Сховати мапу" else "Показати мапу") }
                TextButton(onClick = { showFilters = true }) { Text("Фільтри") }
            }
            OutlinedTextField(value = state.query.search, onValueChange = { onQuery(state.query.copy(search = it)) },
                label = { Text("Пошук за назвою") }, singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }), modifier = Modifier.fillMaxWidth())
            Text("Знайдено: ${state.visible.size} з ${state.telemetry.readings.size} • ${state.query.primary.label} → ${state.query.secondary.label} • " +
                if (state.query.descending) "↓ за спаданням" else "↑ за зростанням",
                style = MaterialTheme.typography.labelMedium, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.testTag("query-summary"))
            state.query.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("query-error")) }
            if (sideBySide && showMap) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SiteList(state, listState, onQuery, onSelect, onDetails, onRestart, Modifier.weight(1f).fillMaxHeight())
                    Box(Modifier.weight(1f).fillMaxHeight().testTag("site-map")) { map() }
                }
            } else {
                if (showMap) Box(Modifier.fillMaxWidth().height(mapHeight).testTag("site-map")) { map() }
                SiteList(state, listState, onQuery, onSelect, onDetails, onRestart, Modifier.weight(1f))
            }
        }
    }
    if (showFilters) ModalBottomSheet(onDismissRequest = { showFilters = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(Modifier.fillMaxWidth().imePadding().padding(16.dp).testTag("filter-list"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Фільтрація та порядок", style = MaterialTheme.typography.titleLarge) }
            item { Text("Усі задані умови діють одночасно. Потужність — встановлена, у кВт.") }
            item { Choice("Тип", listOf<SiteType?>(null) + SiteType.entries, state.query.type, { it?.label ?: "Усі" }) { onQuery(state.query.copy(type = it)) } }
            item { Choice("Статус", listOf<SiteStatus?>(null) + SiteStatus.entries, state.query.status, { it?.label ?: "Усі" }) { onQuery(state.query.copy(status = it)) } }
            item { OutlinedTextField(state.query.minPowerKw, { onQuery(state.query.copy(minPowerKw = it)) }, label = { Text("Мін. встановлена потужність, кВт") }, singleLine = true,
                isError = state.query.error != null, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Next) }), modifier = Modifier.fillMaxWidth().testTag("min-power")) }
            item { OutlinedTextField(state.query.maxPowerKw, { onQuery(state.query.copy(maxPowerKw = it)) }, label = { Text("Макс. встановлена потужність, кВт") }, singleLine = true,
                isError = state.query.error != null, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }), modifier = Modifier.fillMaxWidth().testTag("max-power")) }
            item { state.query.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("filter-error")) } }
            item { Choice("Перший критерій", SiteSort.entries, state.query.primary, { it.label }) { onQuery(state.query.copy(primary = it)) } }
            item { Choice("Другий критерій", SiteSort.entries, state.query.secondary, { it.label }) { onQuery(state.query.copy(secondary = it)) } }
            item { Row(Modifier.fillMaxWidth().testTag("sort-descending").toggleable(value = state.query.descending, role = Role.Checkbox,
                onValueChange = { onQuery(state.query.copy(descending = it)) })) {
                Checkbox(state.query.descending, onCheckedChange = null)
                Text("За спаданням обох критеріїв")
            } }
            item { TextButton(onClick = { focus.clearFocus(); onQuery(SiteQuery()) }) { Text("Скинути") } }
            item { Button(onClick = { focus.clearFocus(); showFilters = false }) { Text("Готово") } }
        }
    }
}

@Composable
private fun SiteList(
    state: ForecastUiState, listState: LazyListState,
    onQuery: (SiteQuery) -> Unit, onSelect: (String) -> Unit,
    onDetails: (String) -> Unit, onRestart: () -> Unit, modifier: Modifier
) {
    LazyColumn(modifier.testTag("site-list"), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.telemetry.phase != StreamPhase.LIVE) {
            item(key = "stream-status") {
                StreamStatus(state.telemetry)
                if (state.telemetry.phase == StreamPhase.FAILED) Button(onClick = onRestart) { Text("Відновити") }
            }
        }
        if (state.visible.isEmpty()) {
            item(key = "empty") {
                if (state.query.error == null && (state.telemetry.phase == StreamPhase.LIVE || state.telemetry.readings.isNotEmpty())) {
                    Text(if (state.telemetry.readings.isEmpty()) "Об'єктів споживання поки немає." else "За цими умовами об'єктів немає.")
                }
                TextButton(onClick = { onQuery(SiteQuery()) }) { Text("Скинути фільтри") }
            }
        }
        // ID, а не позиція чи назва, зберігає ідентичність після сортування/оновлення.
        items(state.visible, key = { it.site.id }) { reading ->
            SiteCard(reading, reading.site.id == state.selectedId, onSelect, onDetails)
        }
    }
}

@Composable
private fun SiteCard(reading: SiteReading, isSelected: Boolean, onSelect: (String) -> Unit, onDetails: (String) -> Unit) {
    val site = reading.site
    // С. 25: Card відкриває окремий екран; «На мапі» лише змінює вибір.
    Card(onClick = { onSelect(site.id); onDetails(site.id) },
        modifier = Modifier.fillMaxWidth().testTag("site-${site.id}").semantics { selected = isSelected },
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(12.dp)) {
            Text(site.name, style = MaterialTheme.typography.titleMedium)
            Text("${site.type.label} • ${site.status.label}")
            Text(site.description)
            Text("Встановлена потужність: ${number(site.ratedPowerKw)} кВт")
            Text("Виміряне навантаження: ${number(reading.measuredPowerKw)} кВт")
            Text("Добовий прогноз: ${number(reading.forecastDailyKwh)} кВт·год")
            Text("Оновлено опис: ${site.updatedAt}", style = MaterialTheme.typography.labelSmall)
            Text("Час виміру: ${reading.sampledAt}", style = MaterialTheme.typography.labelSmall)
            Row {
                TextButton(onClick = { onSelect(site.id) }) { Text("На мапі") }
                TextButton(onClick = { onSelect(site.id); onDetails(site.id) }) { Text("Деталі") }
            }
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
