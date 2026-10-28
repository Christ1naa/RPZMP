package ua.kpi.energyforecast.logic

import ua.kpi.energyforecast.model.ForecastParameters

/**
 * Результат перевірки форми.
 * parameters доступні лише тоді, коли всі поля коректні.
 */
data class ForecastValidation(
    val parameters: ForecastParameters?,
    val errors: Map<String, String>
)

/**
 * Перетворює текстові значення на числа та перевіряє їх.
 */
object ForecastValidator {

    const val POWER = "power"
    const val HOURS = "hours"
    const val COUNT = "count"
    const val DAYS = "days"

    fun validate(
        powerText: String,
        hoursText: String,
        countText: String,
        daysText: String
    ): ForecastValidation {
        val errors = mutableMapOf<String, String>()

        // Підтримуємо десяткову кому та крапку.
        val power = parseDecimal(powerText)
        val hours = parseDecimal(hoursText)
        val count = countText.trim().toIntOrNull()
        val days = daysText.trim().toIntOrNull()

        if (
            power == null ||
            !power.isFinite() ||
            power <= 0.0 ||
            power > 100_000.0
        ) {
            errors[POWER] = "Введіть потужність понад 0 і до 100 000 Вт"
        }

        if (
            hours == null ||
            !hours.isFinite() ||
            hours !in 0.0..24.0
        ) {
            errors[HOURS] = "Введіть час від 0 до 24 годин"
        }

        if (count == null || count !in 1..1000) {
            errors[COUNT] = "Введіть ціле число від 1 до 1000"
        }

        if (days == null || days !in 1..365) {
            errors[DAYS] = "Введіть ціле число від 1 до 365"
        }

        val parameters = if (
            errors.isEmpty() &&
            power != null &&
            hours != null &&
            count != null &&
            days != null
        ) {
            ForecastParameters(
                powerWatts = power,
                hoursPerDay = hours,
                deviceCount = count,
                forecastDays = days
            )
        } else {
            null
        }

        return ForecastValidation(
            parameters = parameters,
            errors = errors
        )
    }

    private fun parseDecimal(text: String): Double? {
        return text
            .trim()
            .replace(',', '.')
            .toDoubleOrNull()
    }
}