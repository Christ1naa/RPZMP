package ua.kpi.energyforecast.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ua.kpi.energyforecast.data.local.EnergyDatabase
import ua.kpi.energyforecast.data.local.LocalScenarioDataSource
import ua.kpi.energyforecast.data.remote.ScenarioRemoteSource
import ua.kpi.energyforecast.model.EnergyDevice
import ua.kpi.energyforecast.model.EnergyScenario
import ua.kpi.energyforecast.model.ScenarioCategory

@RunWith(AndroidJUnit4::class)
class ScenarioCacheTest {

    private lateinit var context: Context
    private lateinit var database: EnergyDatabase
    private lateinit var remote: FakeRemoteSource

    private var now = 1000L

    @Before
    fun setUp() {
        context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        // Окрема тестова база. Робочу energy_forecast.db не змінюємо.
        context.deleteDatabase(TEST_DATABASE)
        database = openDatabase()
        remote = FakeRemoteSource()
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(TEST_DATABASE)
    }

    private fun openDatabase(): EnergyDatabase {
        return Room.databaseBuilder(
            context,
            EnergyDatabase::class.java,
            TEST_DATABASE
        ).build()
    }

    private fun repository(): RoomScenarioRepository {
        return RoomScenarioRepository(
            local = LocalScenarioDataSource(database),
            remote = remote,
            clock = { now }
        )
    }

    @Test
    fun initialCacheIsMissingAndDoesNotCallNetwork() = runBlocking {
        val catalog = repository().observeCatalog().first()

        assertTrue(catalog.scenarios.isEmpty())
        assertNull(catalog.updatedAtMillis)
        assertEquals(0, remote.calls)
    }

    @Test
    fun emptySuccessfulResponseHasUpdateTime() = runBlocking {
        val repository = repository()
        remote.scenarios = emptyList()

        repository.refresh()

        val catalog = repository.observeCatalog().first()

        assertTrue(catalog.scenarios.isEmpty())
        assertEquals(1000L, catalog.updatedAtMillis)
        assertEquals(1, remote.calls)
    }

    @Test
    fun refreshReplacesCatalogAndPreservesDeviceOrder() = runBlocking {
        val repository = repository()

        remote.scenarios = listOf(
            scenario("old"),
            scenario("home-office")
        )
        repository.refresh()

        now = 2000L
        remote.scenarios = listOf(scenario("home-office"))
        repository.refresh()

        val catalog = repository.observeCatalog().first()
        val saved = catalog.scenarios.single()

        assertEquals("home-office", saved.id)
        assertEquals(
            listOf("laptop", "lamp"),
            saved.devices.map { it.id }
        )
        assertEquals(2000L, catalog.updatedAtMillis)
        assertNull(repository.getCachedScenario("old"))

        val forecast = repository.calculateForecast(saved, 30)

        assertEquals(
            0.53,
            forecast.dailyConsumptionKwh,
            0.000001
        )
        assertEquals(
            15.90,
            forecast.periodConsumptionKwh,
            0.000001
        )
    }

    @Test
    fun remoteFailurePreservesDataAndUpdateTime() = runBlocking {
        val repository = repository()
        remote.scenarios = listOf(scenario("home-office"))
        repository.refresh()

        val before = repository.observeCatalog().first()

        now = 2000L
        remote.failure = IOException("Контрольований збій мережі")

        val failure = runCatching {
            repository.refresh()
        }.exceptionOrNull()

        assertTrue(failure is IOException)
        assertEquals(before, repository.observeCatalog().first())
    }

    @Test
    fun cacheSurvivesDatabaseReopeningWithoutNetwork() = runBlocking {
        val firstRepository = repository()
        remote.scenarios = listOf(scenario("home-office"))
        firstRepository.refresh()

        val before = firstRepository.observeCatalog().first()
        val callsBefore = remote.calls

        database.close()
        database = openDatabase()

        remote.failure = IOException("Мережа недоступна")
        val reopenedRepository = repository()

        val after = reopenedRepository.observeCatalog().first()

        assertEquals(before, after)
        assertEquals(callsBefore, remote.calls)
        assertEquals(
            "home-office",
            reopenedRepository.getCachedScenario("home-office")?.id
        )
    }

    @Test
    fun failedDatabaseWriteRollsBackEntireReplacement() = runBlocking {
        val local = LocalScenarioDataSource(database)

        local.replaceCatalog(
            scenarios = listOf(scenario("saved")),
            updatedAtMillis = 1000L
        )

        val before = local.observeCatalog().first()
        val duplicate = scenario("duplicate")

        // Однаковий primary key навмисно спричиняє помилку вставки.
        // Це перевірка транзакції, а не звичайний шлях даних API.
        val failure = runCatching {
            local.replaceCatalog(
                scenarios = listOf(duplicate, duplicate),
                updatedAtMillis = 2000L
            )
        }.exceptionOrNull()

        assertNotNull(failure)
        assertEquals(before, local.observeCatalog().first())
    }

    @Test
    fun activeFlowReceivesUpdatedCatalog() = runBlocking {
        val repository = repository()

        val observation = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(5000) {
                repository.observeCatalog().first { catalog ->
                    catalog.updatedAtMillis == 1000L &&
                            catalog.scenarios.any { it.id == "home-office" }
                }
            }
        }

        try {
            remote.scenarios = listOf(scenario("home-office"))
            repository.refresh()

            val emitted = observation.await()

            assertEquals("home-office", emitted.scenarios.single().id)
        } finally {
            observation.cancel()
        }
    }

    private fun scenario(id: String): EnergyScenario {
        return EnergyScenario(
            id = id,
            name = "Домашній офіс",
            category = ScenarioCategory.OFFICE,
            description = "Контрольний сценарій",
            devices = listOf(
                EnergyDevice(
                    id = "laptop",
                    name = "Ноутбук",
                    powerWatts = 60.0,
                    hoursPerDay = 8.0,
                    deviceCount = 1
                ),
                EnergyDevice(
                    id = "lamp",
                    name = "Лампа",
                    powerWatts = 10.0,
                    hoursPerDay = 5.0,
                    deviceCount = 1
                )
            )
        )
    }

    private class FakeRemoteSource : ScenarioRemoteSource {

        var scenarios: List<EnergyScenario> = emptyList()
        var failure: Exception? = null
        var calls = 0

        override suspend fun loadScenarios(): List<EnergyScenario> {
            calls++
            failure?.let { throw it }
            return scenarios
        }

        override suspend fun loadScenario(id: String): EnergyScenario {
            failure?.let { throw it }
            return scenarios.single { it.id == id }
        }
    }

    companion object {
        private const val TEST_DATABASE = "scenario_cache_test.db"
    }
}