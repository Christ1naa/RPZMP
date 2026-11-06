package ua.kpi.energyforecast.ui.forecast

import ua.kpi.energyforecast.logic.DeviceValidator
import ua.kpi.energyforecast.model.EnergyDevice
import ua.kpi.energyforecast.model.ForecastResult
import java.util.Locale
import ua.kpi.energyforecast.data.source.DemoScenario

/**
 * Зберігає повний список профілю та параметри його відображення.
 * Добовий підсумок надходить із SQL разом зі списком приладів.
 * Пошук і сортування не змінюють дані загального прогнозу.
 */
data class ForecastUiState(
    val devices: List<EnergyDevice> = emptyList(),
    val daysText: String = "30",
    val result: ForecastResult? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val loadError: String? = null,
    val searchQuery: String = "",
    val sort: DeviceSort = DeviceSort.NAME,
    val demoScenarios: List<DemoScenario> = emptyList(),
    val isLoadingDemos: Boolean = false,
    val isImportingDemo: Boolean = false,
    val demoError: String? = null,
    val demoMessage: String? = null,
    val dailyConsumptionKwh: Double = 0.0
) {
    val forecastDays: Int?
        get() = DeviceValidator.parseForecastDays(daysText)

    val totalDeviceCount: Int
        get() = devices.sumOf { it.deviceCount }

    val canAddDevice: Boolean
        get() = devices.size < MAX_DEVICE_POSITIONS

    /**
     * Фільтрує лише відображення; повний список залишається джерелом прогнозу.
     * Нормалізація регістру підтримує українські літери.
     */
    val visibleDevices: List<EnergyDevice>
        get() {
            val query = searchQuery.trim().lowercase(Locale.ROOT)

            val filtered = devices.filter { device ->
                query.isEmpty() ||
                        device.name.lowercase(Locale.ROOT).contains(query)
            }

            val byName = Comparator<EnergyDevice> { first, second ->
                compareNames(first.name, second.name)
            }.thenBy { it.id }

            return when (sort) {
                DeviceSort.NAME -> filtered.sortedWith(byName)

                DeviceSort.CONSUMPTION_DESC -> filtered.sortedWith(
                    compareByDescending<EnergyDevice> {
                        it.powerWatts / 1000.0 *
                                it.hoursPerDay *
                                it.deviceCount
                    }.then(byName)
                )
            }
        }

    companion object {
        const val MAX_DEVICE_POSITIONS = 50

        private const val UKRAINIAN_ALPHABET =
            "абвгґдеєжзиіїйклмнопрстуфхцчшщьюя"

        /** Задає український порядок літер незалежно від системного Collator. */
        private fun compareNames(first: String, second: String): Int {
            val left = first.lowercase(Locale.ROOT)
            val right = second.lowercase(Locale.ROOT)

            for (index in 0 until minOf(left.length, right.length)) {
                val comparison = characterOrder(left[index])
                    .compareTo(characterOrder(right[index]))

                if (comparison != 0) {
                    return comparison
                }
            }

            return left.length.compareTo(right.length)
        }

        private fun characterOrder(character: Char): Int {
            val index = UKRAINIAN_ALPHABET.indexOf(character)

            return if (index >= 0) {
                index
            } else {
                // Інші символи впорядковуються після українських літер.
                UKRAINIAN_ALPHABET.length + character.code
            }
        }
    }
}