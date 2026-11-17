package ua.kpi.energyforecast.pw04.logic

import ua.kpi.energyforecast.pw04.model.*
import java.text.Collator
import java.text.Normalizer
import java.math.BigDecimal
import java.util.Locale

enum class SiteSort(val label: String) {
    NAME("Назва"), TYPE("Тип"), POWER("Встановлена потужність"), DATE("Дата опису"),
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
    // SiteQuery незмінний: розбір виконується один раз, а не для кожного об'єкта списку.
    val minPower = parsePower(minPowerKw)
    val maxPower = parsePower(maxPowerKw)
    val normalizedSearch = normalizeName(search.trim())
    val error: String? = when {
        minPowerKw.isNotBlank() && minPower == null -> "Мінімум має бути скінченним невід'ємним десятковим числом."
        maxPowerKw.isNotBlank() && maxPower == null -> "Максимум має бути скінченним невід'ємним десятковим числом."
        minPower != null && maxPower != null && minPower > maxPower -> "Мінімум перевищує максимум."
        else -> null
    }
}

private val decimalPower = Regex("[+]?(?:[0-9]+(?:[.,][0-9]*)?|[.,][0-9]+)(?:[eE][+-]?[0-9]+)?")
private fun normalizeName(value: String) = Normalizer.normalize(value, Normalizer.Form.NFC)

private fun parsePower(text: String): Double? {
    val value = text.trim()
    if (!decimalPower.matches(value)) return null
    return try {
        val exact = BigDecimal(value.replace(',', '.'))
        exact.toDouble().takeIf { it.isFinite() && (it != 0.0 || exact.signum() == 0) }
    } catch (_: NumberFormatException) { null }
}

/** Фільтри діють одночасно (AND); список і мапа отримують той самий результат. */
fun querySites(readings: List<SiteReading>, query: SiteQuery): List<SiteReading> {
    if (query.error != null) return emptyList()
    val collator = Collator.getInstance(Locale.forLanguageTag("uk-UA")).apply {
        strength = Collator.SECONDARY // Регістр не змінює порядок назв.
        decomposition = Collator.CANONICAL_DECOMPOSITION
    }
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
        normalizeName(it.site.name).contains(query.normalizedSearch, ignoreCase = true) &&
            (query.type == null || it.site.type == query.type) &&
            (query.status == null || it.site.status == query.status) &&
            (query.minPower == null || it.site.ratedPowerKw >= query.minPower) &&
            (query.maxPower == null || it.site.ratedPowerKw <= query.maxPower)
    }.sortedWith(order)
}

fun forecastDaily(powerKw: Double, hours: Double, factor: Double): Double {
    require(powerKw.isFinite() && powerKw >= 0)
    require(hours.isFinite() && hours in 0.0..24.0)
    require(factor.isFinite() && factor in 0.0..2.0)
    return (powerKw * hours * factor).also { require(it.isFinite()) }
}
