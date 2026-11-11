package ua.kpi.energyforecast.pw04.logic

import ua.kpi.energyforecast.pw04.model.*
import java.text.Collator
import java.util.Locale

enum class SiteSort(val label: String) {
    NAME("Назва"), TYPE("Тип"), POWER("Потужність"), DATE("Дата оновлення"),
    STATUS("Статус"), FORECAST("Прогноз")
}

data class SiteQuery(
    val search: String = "",
    val type: SiteType? = null,
    val status: SiteStatus? = null,
    val minPowerKw: String = "",
    val maxPowerKw: String = "",
    val primary: SiteSort = SiteSort.NAME,
    val secondary: SiteSort = SiteSort.POWER,
    val descending: Boolean = false
) {
    private fun parse(text: String) = text.trim().replace(',', '.').toDoubleOrNull()
    val minPower get() = parse(minPowerKw)
    val maxPower get() = parse(maxPowerKw)
    val error: String? get() = when {
        minPowerKw.isNotBlank() && (minPower == null || !minPower!!.isFinite() || minPower!! < 0) -> "Мінімум має бути скінченним невід'ємним числом."
        maxPowerKw.isNotBlank() && (maxPower == null || !maxPower!!.isFinite() || maxPower!! < 0) -> "Максимум має бути скінченним невід'ємним числом."
        minPower != null && maxPower != null && minPower!! > maxPower!! -> "Мінімум перевищує максимум."
        else -> null
    }
}

/** Фільтри діють одночасно (AND); список і мапа отримують той самий результат. */
fun querySites(readings: List<SiteReading>, query: SiteQuery): List<SiteReading> {
    if (query.error != null) return emptyList()
    val collator = Collator.getInstance(Locale.forLanguageTag("uk-UA"))
    fun comparator(sort: SiteSort): Comparator<SiteReading> = when (sort) {
        SiteSort.NAME -> Comparator { a, b -> collator.compare(a.site.name, b.site.name) }
        SiteSort.TYPE -> Comparator { a, b -> collator.compare(a.site.type.label, b.site.type.label) }
        SiteSort.STATUS -> Comparator { a, b -> collator.compare(a.site.status.label, b.site.status.label) }
        SiteSort.POWER -> compareBy { it.site.ratedPowerKw }
        SiteSort.DATE -> compareBy { it.site.updatedAt }
        SiteSort.FORECAST -> compareBy { it.forecastDailyKwh }
    }
    val order = comparator(query.primary).then(comparator(query.secondary))
        .let { if (query.descending) it.reversed() else it }
        .thenBy { it.site.id } // Детермінованість за однакових значень обох критеріїв.
    return readings.filter {
        it.site.name.contains(query.search.trim(), ignoreCase = true) &&
            (query.type == null || it.site.type == query.type) &&
            (query.status == null || it.site.status == query.status) &&
            (query.minPower == null || it.site.ratedPowerKw >= query.minPower!!) &&
            (query.maxPower == null || it.site.ratedPowerKw <= query.maxPower!!)
    }.sortedWith(order)
}

fun forecastDaily(powerKw: Double, hours: Double, factor: Double): Double {
    require(powerKw.isFinite() && powerKw >= 0)
    require(hours.isFinite() && hours in 0.0..24.0)
    require(factor.isFinite() && factor in 0.0..2.0)
    return (powerKw * hours * factor).also { require(it.isFinite()) }
}
