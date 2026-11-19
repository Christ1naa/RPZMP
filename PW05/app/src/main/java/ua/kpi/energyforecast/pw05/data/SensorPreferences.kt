package ua.kpi.energyforecast.pw05.data

import android.content.Context
import ua.kpi.energyforecast.pw05.domain.SensorKind
import ua.kpi.energyforecast.pw05.ui.SensorSettings

class SensorPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("sensors", 0)
    fun read() = SensorSettings(
        selected = SensorKind.entries.filter { prefs.getBoolean("selected_${it.name}", true) }.toSet(),
        thresholds = SensorKind.entries.associateWith { kind ->
            prefs.getFloat("threshold_${kind.name}", kind.defaultThreshold).takeIf { it.isFinite() && it >= 0 } ?: kind.defaultThreshold
        })
    fun write(value: SensorSettings) {
        val edit = prefs.edit()
        SensorKind.entries.forEach { kind ->
            edit.putBoolean("selected_${kind.name}", kind in value.selected)
            edit.putFloat("threshold_${kind.name}", value.thresholds.getValue(kind))
        }
        // Завершуємо запис перед поверненням, щоб негайний reboot не втрачав налаштування.
        check(edit.commit()) { "Не вдалося зберегти налаштування сенсорів" }
    }
}

