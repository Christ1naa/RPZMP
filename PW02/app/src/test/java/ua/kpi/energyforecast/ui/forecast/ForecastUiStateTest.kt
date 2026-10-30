package ua.kpi.energyforecast.ui.forecast

import org.junit.Assert.*
import org.junit.Test
import ua.kpi.energyforecast.model.EnergyDevice

class ForecastUiStateTest {

    @Test
    fun searchFindsUkrainianNamesRegardlessOfCase() {
        val state = ForecastUiState(
            devices = listOf(
                device("1", "Обігрівач"),
                device("2", "Холодильник")
            ),
            searchQuery = "  ОБІГРІ  "
        )

        assertEquals(
            listOf("1"),
            state.visibleDevices.map { it.id }
        )
    }

    @Test
    fun nameSortUsesUkrainianAlphabet() {
        val state = ForecastUiState(
            devices = listOf(
                device("4", "Їжак"),
                device("2", "Ґанок"),
                device("3", "Інструмент"),
                device("1", "Генератор")
            )
        )

        assertEquals(
            listOf("1", "2", "3", "4"),
            state.visibleDevices.map { it.id }
        )
    }

    @Test
    fun consumptionSortAccountsForHoursAndDeviceCount() {
        val state = ForecastUiState(
            devices = listOf(
                device(
                    id = "1",
                    name = "Обігрівач",
                    powerWatts = 2000.0,
                    hoursPerDay = 1.0
                ),
                device(
                    id = "2",
                    name = "Лампи",
                    powerWatts = 100.0,
                    hoursPerDay = 10.0,
                    deviceCount = 3
                )
            ),
            sort = DeviceSort.CONSUMPTION_DESC
        )

        // Лампи: 3 кВт·год/день; обігрівач: 2 кВт·год/день.
        assertEquals(
            listOf("2", "1"),
            state.visibleDevices.map { it.id }
        )
    }

    @Test
    fun unmatchedSearchDoesNotChangeFullListOrCount() {
        val state = ForecastUiState(
            devices = listOf(
                device("1", "Обігрівач", deviceCount = 2)
            ),
            searchQuery = "Телевізор"
        )

        assertTrue(state.visibleDevices.isEmpty())
        assertEquals(1, state.devices.size)
        assertEquals(2, state.totalDeviceCount)
    }

    @Test
    fun filteredListDoesNotBypassPositionLimit() {
        val state = ForecastUiState(
            devices = (1..50).map {
                device(it.toString(), "Лампа $it")
            },
            searchQuery = "Немає збігів"
        )

        assertTrue(state.visibleDevices.isEmpty())
        assertFalse(state.canAddDevice)
    }

    private fun device(
        id: String,
        name: String,
        powerWatts: Double = 1000.0,
        hoursPerDay: Double = 2.0,
        deviceCount: Int = 1
    ) = EnergyDevice(
        id = id,
        name = name,
        powerWatts = powerWatts,
        hoursPerDay = hoursPerDay,
        deviceCount = deviceCount
    )
}