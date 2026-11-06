package ua.kpi.energyforecast.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.math.abs
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ua.kpi.energyforecast.data.local.EnergyDatabase
import ua.kpi.energyforecast.data.source.AssetDemoScenarioSource
import ua.kpi.energyforecast.data.source.CachedDemoScenarioSource
import ua.kpi.energyforecast.model.EnergyDevice

@RunWith(AndroidJUnit4::class)
class RoomEnergyRepositoryTest {

    private lateinit var database: EnergyDatabase
    private lateinit var repository: RoomEnergyRepository

    @Before
    fun setUp() {
        val context: Context =
            InstrumentationRegistry.getInstrumentation().targetContext

        database = Room.inMemoryDatabaseBuilder(
            context,
            EnergyDatabase::class.java
        ).build()

        repository = RoomEnergyRepository(
            database = database,
            demoSource = CachedDemoScenarioSource(
                AssetDemoScenarioSource(context.assets)
            )
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun bundledScenariosAreReadableAndHaveExpectedForecasts() = runBlocking {
        val scenarios = repository.loadDemoScenarios()

        assertEquals(
            setOf("home", "workspace"),
            scenarios.map { it.id }.toSet()
        )

        val home = scenarios.single { it.id == "home" }
        val homeResult = repository.calculateForecast(
            home.devices,
            home.forecastDays
        )

        assertEquals(1.32, homeResult.dailyConsumptionKwh, 0.000001)
        assertEquals(39.6, homeResult.periodConsumptionKwh, 0.000001)

        val workspace = scenarios.single { it.id == "workspace" }
        val workspaceResult = repository.calculateForecast(
            workspace.devices,
            workspace.forecastDays
        )

        assertEquals(2.08, workspaceResult.dailyConsumptionKwh, 0.000001)
        assertEquals(14.56, workspaceResult.periodConsumptionKwh, 0.000001)
    }

    @Test
    fun importPreservesExistingDevicePeriodAndOtherOwner() = runBlocking {
        repository.saveForecastDays(OWNER, 10)
        repository.saveDevice(OWNER, device("existing"))
        repository.saveDevice(OTHER_OWNER, device("other"))

        repository.importDemoScenario(OWNER, "home")

        val saved = repository.observeProfile(OWNER).first()

        assertEquals(4, saved.devices.size)
        assertEquals(10, saved.forecastDays)
        assertTrue(saved.devices.any { it.id == "existing" })

        val other = repository.observeProfile(OTHER_OWNER).first()

        assertEquals(listOf("other"), other.devices.map { it.id })
    }

    @Test
    fun repeatedImportCreatesSeparateDevices() = runBlocking {
        repository.importDemoScenario(OWNER, "home")
        repository.importDemoScenario(OWNER, "home")

        val devices = repository.observeProfile(OWNER).first().devices

        assertEquals(6, devices.size)
        assertEquals(6, devices.map { it.id }.distinct().size)

        assertFalse(
            devices.any {
                it.id in setOf("kettle", "laptop", "lighting")
            }
        )
    }

    @Test
    fun importOverLimitLeavesProfileUnchanged() = runBlocking {
        repository.saveForecastDays(OWNER, 10)

        repeat(49) { index ->
            repository.saveDevice(OWNER, device("existing_$index"))
        }

        val before = repository.observeProfile(OWNER).first()

        val failure = runCatching {
            repository.importDemoScenario(OWNER, "home")
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertEquals(before, repository.observeProfile(OWNER).first())
    }

    @Test
    fun unknownScenarioLeavesProfileUnchanged() = runBlocking {
        repository.saveDevice(OWNER, device("existing"))
        val before = repository.observeProfile(OWNER).first()

        val failure = runCatching {
            repository.importDemoScenario(OWNER, "missing")
        }.exceptionOrNull()

        assertTrue(failure is IllegalArgumentException)
        assertEquals(before, repository.observeProfile(OWNER).first())
    }

    @Test
    fun missingAndEmptyProfilesHaveZeroDailyConsumption() = runBlocking {
        val missing = repository.observeProfile(OWNER).first()
        assertTrue(missing.devices.isEmpty())
        assertEquals(0.0, missing.dailyConsumptionKwh, 0.0)

        repository.saveForecastDays(OWNER, 10)

        val empty = repository.observeProfile(OWNER).first()
        assertTrue(empty.devices.isEmpty())
        assertEquals(10, empty.forecastDays)
        assertEquals(0.0, empty.dailyConsumptionKwh, 0.0)
    }

    @Test
    fun sqlAggregateIncludesAllSixPositionsAndTheirQuantities() = runBlocking {
        val devices = listOf(
            EnergyDevice("a", "Прилад A", 1000.0, 0.5, 1),
            EnergyDevice("b", "Прилад B", 60.0, 4.0, 1),
            EnergyDevice("c", "Прилад C", 10.0, 5.0, 6),
            EnergyDevice("d", "Прилад D", 100.0, 2.0, 2),
            EnergyDevice("e", "Прилад E", 80.0, 3.0, 3),
            EnergyDevice("f", "Прилад F", 100.0, 2.0, 3)
        )
        repository.saveForecastDays(OWNER, 10)
        devices.forEach { repository.saveDevice(OWNER, it) }

        val saved = repository.observeProfile(OWNER).first()
        assertEquals(6, saved.devices.size)
        assertEquals(16, saved.devices.sumOf { it.deviceCount })
        assertEquals(2.76, saved.dailyConsumptionKwh, 1e-9)

        val result = repository.calculateForecastFromDaily(
            saved.dailyConsumptionKwh,
            saved.forecastDays
        )
        assertEquals(27.6, result.periodConsumptionKwh, 1e-9)
    }

    @Test
    fun sqlAggregateFlowUpdatesAfterInsertEditAndDelete() = runBlocking {
        repository.saveForecastDays(OWNER, 10)
        val emissions = Channel<SavedEnergyProfile>(Channel.UNLIMITED)
        val observation = launch {
            repository.observeProfile(OWNER).collect { emissions.send(it) }
        }

        try {
            assertTrue(emissions.awaitDaily(0.0).devices.isEmpty())

            repository.saveDevice(OWNER, device("live"))
            assertEquals(1, emissions.awaitDaily(1.0).devices.size)

            repository.saveDevice(OWNER, device("live").copy(hoursPerDay = 2.0))
            val edited = emissions.awaitDaily(2.0)
            assertEquals(1, edited.devices.size)
            assertEquals(2.0, edited.devices.single().hoursPerDay, 0.0)

            repository.deleteDevice(OWNER, "live")
            assertTrue(emissions.awaitDaily(0.0).devices.isEmpty())
        } finally {
            observation.cancelAndJoin()
            emissions.close()
        }
    }

    @Test
    fun sqlAggregateIsIsolatedByOwnerEvenWithMatchingDeviceIds() = runBlocking {
        repository.saveDevice(OWNER, device("shared"))
        repository.saveDevice(
            OWNER,
            device("idle").copy(hoursPerDay = 0.0, deviceCount = 10)
        )
        repository.saveDevice(
            OTHER_OWNER,
            device("shared").copy(deviceCount = 9)
        )

        assertEquals(
            1.0,
            repository.observeProfile(OWNER).first().dailyConsumptionKwh,
            1e-9
        )
        assertEquals(
            9.0,
            repository.observeProfile(OTHER_OWNER).first().dailyConsumptionKwh,
            1e-9
        )

        repository.deleteDevice(OTHER_OWNER, "shared")
        assertEquals(
            1.0,
            repository.observeProfile(OWNER).first().dailyConsumptionKwh,
            1e-9
        )
        assertEquals(
            0.0,
            repository.observeProfile(OTHER_OWNER).first().dailyConsumptionKwh,
            0.0
        )
    }

    private suspend fun Channel<SavedEnergyProfile>.awaitDaily(
        expected: Double
    ): SavedEnergyProfile = withTimeout(5_000) {
        var saved = receive()
        while (abs(saved.dailyConsumptionKwh - expected) > 1e-9) {
            saved = receive()
        }
        saved
    }

    private fun device(id: String) = EnergyDevice(
        id = id,
        name = "Тестовий прилад",
        powerWatts = 1000.0,
        hoursPerDay = 1.0,
        deviceCount = 1
    )

    companion object {
        private const val OWNER = "demo@energy.test"
        private const val OTHER_OWNER = "other@energy.test"
    }
}