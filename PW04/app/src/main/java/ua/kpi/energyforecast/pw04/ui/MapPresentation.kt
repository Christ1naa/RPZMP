package ua.kpi.energyforecast.pw04.ui

import androidx.compose.runtime.*
import ua.kpi.energyforecast.pw04.model.Coordinates
import ua.kpi.energyforecast.pw04.model.SiteReading

data class SiteMarkerPresentation(
    val id: String, val position: Coordinates, val title: String,
    val snippet: String, val isSelected: Boolean
)

/** Тільки visible-колекція; ID і координати лишаються тими самими після сортування. */
fun siteMarkers(readings: List<SiteReading>, selectedId: String?): List<SiteMarkerPresentation> = readings.map {
    val site = it.site
    SiteMarkerPresentation(site.id, site.position, site.name,
        "${site.type.label} • ${site.status.label}\n${site.description}\n" +
            "${number(site.ratedPowerKw)} кВт • ${number(it.forecastDailyKwh)} кВт·год/добу",
        site.id == selectedId)
}

/** Тестований контракт фокусу, який використовує справжня Google Maps камера.
 * Новий вимір/нова лямбда не є дією користувача; нова revision є такою дією.
 * До готовності мапи зберігається тільки останній актуальний вибір.
 */
@Composable
internal fun SiteFocusEffect(
    readings: List<SiteReading>, selectedId: String?, selectionRevision: Long,
    ready: Boolean, onFocus: (Coordinates) -> Unit
) {
    val position = readings.firstOrNull { it.site.id == selectedId }?.site?.position
    val focus by rememberUpdatedState(onFocus)
    LaunchedEffect(selectedId, selectionRevision, ready, position) {
        if (ready && position != null) focus(position)
    }
}
