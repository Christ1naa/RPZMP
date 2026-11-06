package ua.kpi.doctrack.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

/** Єдиний підхід до роботи з датами: у формах введення, при перевірці даних та під час відображення дат у записах. */
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
