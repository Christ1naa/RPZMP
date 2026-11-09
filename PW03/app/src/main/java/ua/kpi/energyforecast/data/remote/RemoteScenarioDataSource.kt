package ua.kpi.energyforecast.data.remote

import ua.kpi.energyforecast.model.EnergyScenario

/** Отримує дані API та повертає лише перевірені моделі. */
class RemoteScenarioDataSource(
    private val api: EnergyApi
) {

    suspend fun loadScenarios(): List<EnergyScenario> {
        return api.getScenarios().toModels()
    }

    suspend fun loadScenario(id: String): EnergyScenario {
        require(id.isNotBlank()) {
            "ID сценарію не може бути порожнім"
        }

        val scenario = api.getScenario(id).toModel()

        if (scenario.id != id) {
            throw InvalidApiDataException(
                "ID отриманого сценарію не відповідає запиту"
            )
        }

        return scenario
    }
}