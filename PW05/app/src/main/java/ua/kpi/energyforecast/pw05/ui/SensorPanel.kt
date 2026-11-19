package ua.kpi.energyforecast.pw05.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import ua.kpi.energyforecast.pw05.domain.*
import java.util.Locale

@Composable
fun SensorPanel(state: SensorPanelState, select: (SensorKind, Boolean) -> Unit, threshold: (SensorKind, Float) -> Unit) {
    var type by rememberSaveable { mutableStateOf(-1) }
    var minimum by rememberSaveable { mutableStateOf("") }
    var maximum by rememberSaveable { mutableStateOf("") }
    var sort by rememberSaveable { mutableStateOf(SensorSort.TIME) }
    var descending by rememberSaveable { mutableStateOf(true) }
    fun parse(text: String) = text.trim().replace(',', '.').toFloatOrNull()?.takeIf { it.isFinite() }
    val low = parse(minimum); val high = parse(maximum)
    val invalid = (minimum.isNotBlank() && low == null) || (maximum.isNotBlank() && high == null) || (low != null && high != null && low > high)
    Text("Моніторинг сенсорів", style = MaterialTheme.typography.titleLarge)
    Text("Реальні датчики телефона. Рух і світло не є вимірами споживання електроенергії. Значення векторів порівнюються за модулем.")
    SensorKind.entries.forEach { kind ->
        Row { Checkbox(kind in state.settings.selected, { select(kind, it) }, modifier = Modifier.semantics { contentDescription = "Вибір: ${kind.title}" }); Text(kind.title) }
        if (kind in state.settings.selected) {
            val snapshot = state.snapshots.find { it.kind == kind }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(when (snapshot?.status) {
                        SensorStatus.LIVE -> "Дані надходять"
                        SensorStatus.STALE -> "Немає нових вимірів понад 5 с; показано останній відомий"
                        SensorStatus.MISSING -> "Датчик відсутній на пристрої"
                        SensorStatus.FAILED -> "Не вдалося підключити датчик"
                        SensorStatus.INVALID -> "Некоректний або неупорядкований вимір відхилено"
                        else -> "Очікування першого виміру"
                    })
                    snapshot?.readings?.lastOrNull()?.let { reading ->
                        Text("${kind.title}: ${format(reading.magnitude)} ${kind.unit}")
                        Text("Компоненти: ${reading.values.joinToString { format(it) }}")
                        Text("Час від запуску пристрою: ${reading.timestampNanos / 1_000_000} мс")
                        if (reading.magnitude > state.settings.thresholds.getValue(kind)) Text("Перевищено поріг", color = MaterialTheme.colorScheme.error)
                    }
                    var draft by rememberSaveable(kind.name) { mutableStateOf(state.settings.thresholds.getValue(kind).toString()) }
                    val value = parse(draft)
                    OutlinedTextField(draft, { draft = it; parse(it)?.takeIf { v -> v >= 0 }?.let { v -> threshold(kind, v) } }, label = { Text("Поріг (${kind.unit})") }, isError = value == null || value < 0, modifier = Modifier.fillMaxWidth())
                    Text("Поточний поріг: ${format(state.settings.thresholds.getValue(kind))} ${kind.unit}. Системні події: увімкніть категорію «Сенсори» у сповіщеннях.")
                }
            }
        }
    }
    Text("Історія: до 120 вимірів кожного сенсора (10/с)")
    Button(onClick = { type = if (type == SensorKind.entries.lastIndex) -1 else type + 1 }) { Text("Тип: ${SensorKind.entries.getOrNull(type)?.title ?: "Усі"}") }
    OutlinedTextField(minimum, { minimum = it }, label = { Text("Мінімальний модуль") }, modifier = Modifier.fillMaxWidth(), isError = invalid)
    OutlinedTextField(maximum, { maximum = it }, label = { Text("Максимальний модуль") }, modifier = Modifier.fillMaxWidth(), isError = invalid)
    Text("Межі застосовуються в одиницях кожного сенсора; для фізичного порівняння оберіть один тип.")
    Button(onClick = { sort = SensorSort.entries[(sort.ordinal + 1) % SensorSort.entries.size] }) { Text("Сортування: ${when(sort) { SensorSort.TYPE -> "тип"; SensorSort.VALUE -> "модуль"; SensorSort.TIME -> "час" }}") }
    Row { Checkbox(descending, { descending = it }); Text("За спаданням") }
    if (invalid) Text("Введіть скінченні числа; мінімум не має перевищувати максимум", color = MaterialTheme.colorScheme.error)
    else {
        val rows = SensorQuery(SensorKind.entries.getOrNull(type), low, high, sort, descending).apply(state.snapshots.flatMap { it.readings })
        Text("Збігів: ${rows.size}")
        // Текстова таблиця має явні одиниці й час, доступна без графічного декодування.
        var page by rememberSaveable { mutableIntStateOf(0) }
        val lastPage = ((rows.size - 1) / 20).coerceAtLeast(0)
        val current = page.coerceIn(0, lastPage)
        Text("Сторінка ${current + 1} / ${lastPage + 1}")
        Row {
            TextButton({ page = current - 1 }, enabled = current > 0) { Text("Назад") }
            TextButton({ page = current + 1 }, enabled = current < lastPage) { Text("Далі") }
        }
        rows.drop(current * 20).take(20).forEach { Text("${it.kind.title} | ${format(it.magnitude)} ${it.kind.unit} | ${it.timestampNanos / 1_000_000} мс") }
    }
}
private fun format(value: Float) = String.format(Locale.forLanguageTag("uk-UA"), "%.2f", value)
