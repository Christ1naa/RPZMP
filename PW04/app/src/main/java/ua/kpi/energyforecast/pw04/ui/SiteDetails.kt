package ua.kpi.energyforecast.pw04.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ua.kpi.energyforecast.pw04.data.StreamPhase
import ua.kpi.energyforecast.pw04.data.TelemetryState

/** ID зі шляху шукається у повному наборі: фільтри каталогу не змінюють об'єкт деталей. */
@Composable
fun SiteDetails(siteId: String?, telemetry: TelemetryState, onBack: () -> Unit, onRestart: () -> Unit) {
    val reading = telemetry.readings.firstOrNull { it.site.id == siteId }
    Column(Modifier.fillMaxSize().padding(16.dp).testTag("site-details")) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onBack) { Text("Назад") }
            Text("Деталі об'єкта", style = MaterialTheme.typography.titleLarge)
        }
        LazyColumn(Modifier.weight(1f).testTag("details-list"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (telemetry.phase != StreamPhase.LIVE) item(key = "stream") {
                StreamStatus(telemetry)
                if (telemetry.phase == StreamPhase.FAILED) Button(onClick = onRestart) { Text("Відновити") }
            }
            if (reading == null) {
                item(key = "missing") {
                    Text(when (telemetry.phase) {
                        StreamPhase.WAITING, StreamPhase.RETRYING -> "Очікуємо дані об'єкта…"
                        StreamPhase.FAILED -> "Не вдалося отримати дані об'єкта. Спробуйте відновити збір."
                        StreamPhase.LIVE -> "Об'єкт не знайдено. Поверніться до каталогу."
                    }, modifier = Modifier.testTag("details-empty"))
                }
            } else {
                val site = reading.site
                item(key = "name") { Text(site.name, style = MaterialTheme.typography.headlineSmall) }
                item(key = "type") { Text("Тип: ${site.type.label}") }
                item(key = "status") { Text("Статус: ${site.status.label}") }
                item(key = "description") { Text(site.description) }
                item(key = "rated-power") { Text("Встановлена потужність: ${number(site.ratedPowerKw)} кВт") }
                item(key = "hours") { Text("Режим: ${number(site.hoursPerDay)} год/добу") }
                item(key = "measured-power") { Text("Виміряне навантаження: ${number(reading.measuredPowerKw)} кВт") }
                item(key = "forecast") { Text("Добовий прогноз: ${number(reading.forecastDailyKwh)} кВт·год") }
                item(key = "updated") { Text("Оновлено опис: ${site.updatedAt}") }
                item(key = "sampled") { Text("Час виміру: ${reading.sampledAt}") }
                item(key = "position") { Text("Координати: ${site.position.latitude}, ${site.position.longitude}") }
                item(key = "disclaimer") { Text("Об'єкт і координатна прив'язка вигадані.") }
            }
        }
    }
}
