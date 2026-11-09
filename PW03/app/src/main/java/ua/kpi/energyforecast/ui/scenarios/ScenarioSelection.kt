package ua.kpi.energyforecast.ui.scenarios

import java.text.Collator
import java.util.Locale
import ua.kpi.energyforecast.logic.EnergyCalculator
import ua.kpi.energyforecast.model.EnergyScenario
import ua.kpi.energyforecast.model.ScenarioCategory

enum class ScenarioSort {
    NAME_ASC,
    DAILY_ASC,
    DAILY_DESC
}

/** Дані картки: сценарій і його розраховане добове споживання. */
data class ScenarioListItem(
    val scenario: EnergyScenario,
    val dailyConsumptionKwh: Double
)

/**
 * Змінює лише відображення каталогу.
 * Пошук і фільтри не змінюють збережені сценарії або їх прилади.
 */
fun selectScenarios(
    scenarios: List<EnergyScenario>,
    query: String,
    category: ScenarioCategory?,
    sort: ScenarioSort
): List<ScenarioListItem> {
    val normalizedQuery = query.trim()

    val items = scenarios
        .asSequence()
        .filter { scenario ->
            normalizedQuery.isEmpty() ||
                    scenario.name.contains(
                        normalizedQuery,
                        ignoreCase = true
                    )
        }
        .filter { scenario ->
            category == null || scenario.category == category
        }
        .map { scenario ->
            ScenarioListItem(
                scenario = scenario,
                dailyConsumptionKwh = EnergyCalculator.calculateTotal(
                    devices = scenario.devices,
                    forecastDays = 1
                ).dailyConsumptionKwh
            )
        }
        .toList()

    val collator = Collator.getInstance(
        Locale.forLanguageTag("uk-UA")
    ).apply {
        strength = Collator.PRIMARY
    }

    val nameComparator = Comparator<ScenarioListItem> { first, second ->
        collator.compare(first.scenario.name, second.scenario.name)
    }.thenBy { it.scenario.id }

    return when (sort) {
        ScenarioSort.NAME_ASC -> items.sortedWith(nameComparator)

        ScenarioSort.DAILY_ASC -> items.sortedWith(
            compareBy<ScenarioListItem> { it.dailyConsumptionKwh }
                .then(nameComparator)
        )

        ScenarioSort.DAILY_DESC -> items.sortedWith(
            compareByDescending<ScenarioListItem> {
                it.dailyConsumptionKwh
            }.then(nameComparator)
        )
    }
}