package ua.kpi.energyforecast.data.remote

import ua.kpi.energyforecast.logic.DeviceValidator
import ua.kpi.energyforecast.model.EnergyDevice
import ua.kpi.energyforecast.model.EnergyScenario
import ua.kpi.energyforecast.model.ScenarioCategory

class InvalidApiDataException(message: String) :
    IllegalArgumentException(message)

/** Перевіряє весь список до його використання або збереження. */
fun ScenarioListDto.toModels(): List<EnergyScenario> {
    val source = scenarios
        ?: invalid("У відповіді відсутній список scenarios")

    val result = source.map { dto ->
        (dto ?: invalid("Список містить null замість сценарію")).toModel()
    }

    if (result.map { it.id }.distinct().size != result.size) {
        invalid("Список містить повторні ID сценаріїв")
    }

    return result
}

fun ScenarioDto.toModel(): EnergyScenario {
    val scenarioId = id.requiredText("scenario.id")
    val scenarioName = name.requiredText("scenario.name")
    val categoryValue = category.requiredText("scenario.category")

    val scenarioCategory = ScenarioCategory.entries.firstOrNull {
        it.name == categoryValue
    } ?: invalid("Невідома категорія сценарію")

    val scenarioDescription = description
        ?: invalid("Відсутній опис сценарію")

    val sourceDevices = devices
        ?: invalid("Відсутній список приладів")

    if (sourceDevices.size !in 1..50) {
        invalid("Сценарій має містити від 1 до 50 позицій")
    }

    val mappedDevices = sourceDevices.map { dto ->
        (dto ?: invalid("Список містить null замість приладу")).toModel()
    }

    if (mappedDevices.map { it.id }.distinct().size != mappedDevices.size) {
        invalid("Сценарій містить повторні ID приладів")
    }

    return EnergyScenario(
        id = scenarioId,
        name = scenarioName,
        category = scenarioCategory,
        description = scenarioDescription,
        devices = mappedDevices
    )
}

private fun ScenarioDeviceDto.toModel(): EnergyDevice {
    val deviceId = id.requiredText("device.id")
    val deviceName = name.requiredText("device.name")
    val power = powerWatts ?: invalid("Відсутня потужність")
    val hours = hoursPerDay ?: invalid("Відсутній час роботи")
    val count = deviceCount ?: invalid("Відсутня кількість")

    // Правила API-приладів збігаються з правилами локальної форми.
    val validation = DeviceValidator.validate(
        nameText = deviceName,
        powerText = power.toString(),
        hoursText = hours.toString(),
        countText = count.toString()
    )

    if (validation.parameters == null) {
        invalid("Некоректні параметри приладу: $deviceId")
    }

    return EnergyDevice(
        id = deviceId,
        name = validation.name,
        powerWatts = power,
        hoursPerDay = hours,
        deviceCount = count
    )
}

private fun String?.requiredText(field: String): String {
    return this?.trim()?.takeIf { it.isNotEmpty() }
        ?: invalid("Відсутнє або порожнє поле: $field")
}

private fun invalid(message: String): Nothing {
    throw InvalidApiDataException(message)
}