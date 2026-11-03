package ua.kpi.doctrack.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/** One calendar convention for form input, validation and displayed record dates. */
object RecordDates {
    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.uuuu")
        .withResolverStyle(ResolverStyle.STRICT)

    fun isSupported(date: LocalDate): Boolean = date.year in 1..9999

    fun parse(text: String): LocalDate? {
        if (!Regex("[0-9]{2}\\.[0-9]{2}\\.[0-9]{4}").matches(text)) return null
        return runCatching { LocalDate.parse(text, formatter) }
            .getOrNull()?.takeIf(::isSupported)
    }
}
