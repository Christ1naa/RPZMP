package ua.kpi.energyforecast.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
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