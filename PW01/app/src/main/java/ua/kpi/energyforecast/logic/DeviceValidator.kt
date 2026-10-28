package ua.kpi.energyforecast.logic

import ua.kpi.energyforecast.model.ForecastParameters

data class DeviceValidation(
    val name: String,
    val parameters: ForecastParameters?,
    val errors: Map<String, String>
)

object DeviceValidator {

    const val NAME = "name"

    fun validate(
        nameText: String,
        powerText: String,
        hoursText: String,
        countText: String
    ): DeviceValidation {
        val name = nameText.trim()

        val numericValidation = ForecastValidator.validate(
            powerText = powerText,
            hoursText = hoursText,
            countText = countText,
            daysText = "1"
        )

        val errors = numericValidation.errors.toMutableMap()

        if (name.isEmpty() || name.length > 60) {
            errors[NAME] = "Введіть назву від 1 до 60 символів"
        }

        return DeviceValidation(
            name = name,
            parameters = if (errors.isEmpty()) {
                numericValidation.parameters
            } else {
                null
            },
            errors = errors
        )
    }

    fun parseForecastDays(text: String): Int? {
        return text.trim()
            .toIntOrNull()
            ?.takeIf { it in 1..365 }
    }
}