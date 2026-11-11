package ua.kpi.energyforecast.pw04.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.kpi.energyforecast.pw04.data.*
import ua.kpi.energyforecast.pw04.model.SiteReading
import java.util.Locale

fun number(value: Double) = String.format(Locale.forLanguageTag("uk-UA"), "%.2f", value)

@Composable
fun StreamStatus(state: TelemetryState) {
    Text(when (state.phase) {
        StreamPhase.WAITING -> "Очікування даних…"
        StreamPhase.LIVE -> "Дані надходять • повторів: ${state.retries}"
        StreamPhase.RETRYING -> "Збій лічильника • повтор ${state.retries}/3 • останні дані застарілі"
        StreamPhase.FAILED -> "Потік зупинено через помилку. Натисніть «Відновити»."
    }, color = if (state.phase in listOf(StreamPhase.RETRYING, StreamPhase.FAILED)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
}

@Composable
fun MonitorScreen(state: TelemetryState, onFailure: () -> Unit, onRestart: () -> Unit) {
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
                Text("${number(state.readings.sumOf { it.forecastDailyKwh })} кВт·год", style = MaterialTheme.typography.headlineMedium)
                Text("Навантаження: ${number(state.readings.sumOf { it.measuredPowerKw })} кВт")
                Text("Коефіцієнт моделі: ${state.factor?.let(::number) ?: "—"}")
            } }
        }
        item {
            Text("Прогноз = виміряні кВт × години роботи за добу × коефіцієнт попиту. " +
                "Це демонстраційна модель сталого режиму, а не показники реальних лічильників.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onFailure) { Text("Імітувати збій") }
                Button(onClick = onRestart) { Text("Відновити") }
            }
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

@Composable
fun SiteDetails(reading: SiteReading?, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Button(onClick = onBack) { Text("Назад") } }
        if (reading == null) item { Text("Об'єкт недоступний або дані ще завантажуються.") }
        else item {
            val site = reading.site
            Text(site.name, style = MaterialTheme.typography.headlineSmall)
            Text("${site.type.label} • ${site.status.label}")
            Text(site.description)
            Text("Встановлена потужність: ${number(site.ratedPowerKw)} кВт")
            Text("Режим: ${number(site.hoursPerDay)} год/добу")
            Text("Виміряне навантаження: ${number(reading.measuredPowerKw)} кВт")
            Text("Добовий прогноз: ${number(reading.forecastDailyKwh)} кВт·год")
            Text("Оновлено опис: ${site.updatedAt}")
            Text("Час виміру: ${reading.sampledAt}")
            Text("Координати: ${site.position.latitude}, ${site.position.longitude}")
            Text("Об'єкт і координатна прив'язка вигадані.")
        }
    }
}
