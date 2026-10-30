package ua.kpi.energyforecast.data.source

import android.content.res.AssetManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import ua.kpi.energyforecast.logic.DeviceValidator
import ua.kpi.energyforecast.model.EnergyDevice

/** Читає приклади з assets і перевіряє їх до передачі в Repository. */
class AssetDemoScenarioSource(
    private val assets: AssetManager,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : DemoScenarioSource {

    override suspend fun load(): List<DemoScenario> =
        withContext(dispatcher) {
            val text = assets.open("demo_scenarios.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            val array = JSONArray(text)

            require(array.length() > 0) {
                "Джерело не містить прикладів."
            }

            val scenarios = List(array.length()) { index ->
                parseScenario(array.getJSONObject(index))
            }

            require(scenarios.map { it.id }.distinct().size == scenarios.size) {
                "Ідентифікатори прикладів повторюються."
            }

            scenarios
        }

    private fun parseScenario(json: JSONObject): DemoScenario {
        val id = json.getString("id").trim()
        val name = json.getString("name").trim()
        val days = readInt(json, "forecastDays")
        val array = json.getJSONArray("devices")

        require(id.isNotBlank()) {
            "Не задано ідентифікатор прикладу."
        }
        require(name.length in 1..60) {
            "Назва прикладу має містити від 1 до 60 символів."
        }
        require(days in 1..365) {
            "Період прикладу має бути від 1 до 365 днів."
        }
        require(array.length() in 1..50) {
            "Приклад має містити від 1 до 50 позицій."
        }

        val devices = List(array.length()) { index ->
            parseDevice(array.getJSONObject(index))
        }

        require(devices.map { it.id }.distinct().size == devices.size) {
            "Ідентифікатори приладів у прикладі повторюються."
        }

        return DemoScenario(
            id = id,
            name = name,
            forecastDays = days,
            devices = devices
        )
    }

    private fun parseDevice(json: JSONObject): EnergyDevice {
        val id = json.getString("id").trim()
        val name = json.getString("name")
        val power = json.getDouble("powerWatts")
        val hours = json.getDouble("hoursPerDay")
        val count = readInt(json, "deviceCount")

        val validation = DeviceValidator.validate(
            nameText = name,
            powerText = power.toString(),
            hoursText = hours.toString(),
            countText = count.toString()
        )

        require(id.isNotBlank() && validation.parameters != null) {
            "Некоректні параметри приладу в прикладі."
        }

        return EnergyDevice(
            id = id,
            name = validation.name,
            powerWatts = power,
            hoursPerDay = hours,
            deviceCount = count
        )
    }

    /** Відхиляє дробове значення замість його неявного округлення. */
    private fun readInt(json: JSONObject, key: String): Int =
        requireNotNull(json.get(key).toString().toIntOrNull()) {
            "Поле $key має містити ціле число."
        }
}