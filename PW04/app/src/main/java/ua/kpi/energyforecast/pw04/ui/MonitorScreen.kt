package ua.kpi.energyforecast.pw04.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ua.kpi.energyforecast.pw04.data.*
import java.util.Locale

fun number(value: Double) = String.format(Locale.forLanguageTag("uk-UA"), "%.2f", value)

@Composable
fun StreamStatus(state: TelemetryState) {
    Column(Modifier.testTag("stream-status")) {
        Text(when (state.phase) {
            StreamPhase.WAITING -> "Очікування даних…"
            StreamPhase.LIVE -> "Дані надходять • повторів: ${state.retries}"
            StreamPhase.RETRYING -> "Збір відновлюється автоматично."
            StreamPhase.FAILED -> "Автовідновлення зупинено. Натисніть «Відновити»."
        })
        listOf("Лічильник" to state.meterStatus, "Модель" to state.forecastStatus).forEach { (label, status) ->
            val reason = when (status.issue) {
                StreamIssue.NO_DATA -> "Немає валідних даних"
                StreamIssue.IO -> "Збій джерела"
                StreamIssue.SOURCE_ERROR -> "Помилка джерела"
                null -> ""
            }
            Text("$label: " + when (status.phase) {
                StreamPhase.WAITING -> "очікування"
                StreamPhase.LIVE -> "дані надходять"
                StreamPhase.RETRYING -> "$reason • повтор ${status.retries}/${status.maxRetries}"
                StreamPhase.FAILED -> "$reason • збір зупинено"
            }, color = if (status.issue != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        }
        if (state.isStale) Text("Останні дані застарілі", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
fun MonitorScreen(state: TelemetryState, onFailure: () -> Unit, onRestart: () -> Unit, onForecastFailure: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("EnergyForecast • PW04", style = MaterialTheme.typography.headlineSmall)
            Text("Система прогнозування споживання електроенергії")
            Text("Навчальна симуляція • вигадані дані", style = MaterialTheme.typography.labelLarge)
        }
        item { StreamStatus(state) }
        item {
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Text("Прогноз за добу", style = MaterialTheme.typography.titleMedium)
                Text(if (state.readings.isEmpty()) "Немає даних" else "${number(state.readings.sumOf { it.forecastDailyKwh })} кВт·год", style = MaterialTheme.typography.headlineMedium)
                Text("Навантаження: " + if (state.readings.isEmpty()) "—" else "${number(state.readings.sumOf { it.measuredPowerKw })} кВт")
                Text("Коефіцієнт моделі: ${state.factor?.let(::number) ?: "—"}")
            } }
        }
        item {
            Text("Прогноз = виміряні кВт × години роботи за добу × коефіцієнт попиту. " +
                "Це демонстраційна модель сталого режиму, а не показники реальних лічильників.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onFailure) { Text("Збій лічильника") }
                OutlinedButton(onClick = onForecastFailure) { Text("Збій моделі") }
            }
            Button(onClick = onRestart) { Text("Відновити") }
        }
        items(state.readings, key = { it.site.id }) { reading ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                Text(reading.site.name, style = MaterialTheme.typography.titleMedium)
                Text("${number(reading.measuredPowerKw)} кВт → ${number(reading.forecastDailyKwh)} кВт·год/добу")
                Text("Вимір: ${reading.sampledAt}")
            } }
        }
    }
}
