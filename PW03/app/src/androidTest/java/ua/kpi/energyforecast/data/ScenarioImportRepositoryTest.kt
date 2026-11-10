package ua.kpi.energyforecast.data

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
import ua.kpi.energyforecast.data.local.LocalScenarioDataSource
import ua.kpi.energyforecast.data.local.toEntity
import ua.kpi.energyforecast.data.remote.ScenarioRemoteSource
import ua.kpi.energyforecast.model.EnergyDevice
import ua.kpi.energyforecast.model.EnergyScenario
import ua.kpi.energyforecast.model.ScenarioCategory

@RunWith(AndroidJUnit4::class)
class ScenarioImportRepositoryTest {

    private lateinit var database: EnergyDatabase
    private lateinit var local: LocalScenarioDataSource
    private lateinit var scenarios: RoomScenarioRepository
    private lateinit var importer: ScenarioImportRepository

    private val scenario = EnergyScenario(
        id = "home-office",
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

    @Before
    fun setUp() {
        val context = InstrumentationRegistry
            .getInstrumentation()
            .targetContext

        // Окрема база в пам'яті не змінює дані застосунку.
        database = Room.inMemoryDatabaseBuilder(
            context,
            EnergyDatabase::class.java
        ).build()

        local = LocalScenarioDataSource(database)

        scenarios = RoomScenarioRepository(
            local = local,
            remote = object : ScenarioRemoteSource {
                override suspend fun loadScenarios():
                        List<EnergyScenario> {
                    error("Імпорт не повинен звертатися до мережі")
                }

                override suspend fun loadScenario(
                    id: String
                ): EnergyScenario {
                    error("Імпорт не повинен звертатися до мережі")
                }
            }
        )

        importer = ScenarioImportRepository(database, scenarios)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun prepareProfile(deviceCount: Int) {
        local.replaceCatalog(
            scenarios = listOf(scenario),
            updatedAtMillis = 1000L
        )

        val dao = database.energyDao()
        dao.saveForecastDays(OWNER, 10)

        repeat(deviceCount) { index ->
            dao.saveDevice(
                EnergyDevice(
                    id = "existing_$index",
                    name = "Наявний прилад $index",
                    powerWatts = 100.0,
                    hoursPerDay = 1.0,
                    deviceCount = 1
                ).toEntity(OWNER)
            )
        }
    }

    @Test
    fun importPreservesProfileOtherOwnerAndCachedScenario() =
        runBlocking {
            prepareProfile(deviceCount = 1)

            val dao = database.energyDao()
            val otherDevice = EnergyDevice(
                id = "other-device",
                name = "Інший профіль",
                powerWatts = 50.0,
                hoursPerDay = 2.0,
                deviceCount = 1
            ).toEntity(OTHER_OWNER)

            dao.saveDevice(otherDevice)

            val otherBefore = dao.observeProfile(OTHER_OWNER).first()
            val catalogBefore = scenarios.observeCatalog().first()

            importer.importScenario(OWNER, scenario.id)

            val saved = requireNotNull(
                dao.observeProfile(OWNER).first()
            )

            assertEquals(10, saved.profile.forecastDays)
            assertEquals(3, saved.devices.size)
            assertTrue(
                saved.devices.any { it.id == "existing_0" }
            )

            val imported = saved.devices.filter {
                it.id != "existing_0"
            }

            assertEquals(
                setOf("Ноутбук", "Лампа"),
                imported.map { it.name }.toSet()
            )
            assertEquals(2, imported.map { it.id }.distinct().size)
            assertFalse(
                imported.any { device ->
                    device.id in scenario.devices.map { it.id }
                }
            )
            assertTrue(imported.all { it.ownerKey == OWNER })

            // 0,10 від наявного приладу + 0,53 від сценарію.
            assertEquals(0.63, saved.dailyConsumptionKwh, 1e-9)

            assertEquals(
                otherBefore,
                dao.observeProfile(OTHER_OWNER).first()
            )
            assertEquals(
                catalogBefore,
                scenarios.observeCatalog().first()
            )
        }

    @Test
    fun importAllowsExactlyFiftyPositions() = runBlocking {
        prepareProfile(deviceCount = 48)

        importer.importScenario(OWNER, scenario.id)

        val saved = requireNotNull(
            database.energyDao().observeProfile(OWNER).first()
        )

        assertEquals(50, saved.devices.size)
        assertEquals(10, saved.profile.forecastDays)
    }

    @Test
    fun importOverLimitLeavesEntireProfileUnchanged() =
        runBlocking {
            prepareProfile(deviceCount = 49)

            val dao = database.energyDao()
            val before = dao.observeProfile(OWNER).first()

            val failure = runCatching {
                importer.importScenario(OWNER, scenario.id)
            }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertEquals(
                before,
                dao.observeProfile(OWNER).first()
            )
        }

    @Test
    fun failureOnSecondDeviceRollsBackFirstInsert() =
        runBlocking {
            prepareProfile(deviceCount = 1)

            val dao = database.energyDao()
            val before = dao.observeProfile(OWNER).first()
            val catalogBefore = scenarios.observeCatalog().first()

            // Збій виникає на другому приладі лише після вставлення
            // першого. Це перевіряє відкат уже розпочатого запису.
            database.openHelper.writableDatabase.execSQL(
                """
                CREATE TRIGGER fail_second_imported_device
                BEFORE INSERT ON energy_devices
                WHEN NEW.name = 'Лампа'
                  AND EXISTS (
                    SELECT 1
                    FROM energy_devices
                    WHERE ownerKey = NEW.ownerKey
                      AND name = 'Ноутбук'
                  )
                BEGIN
                    SELECT RAISE(ABORT, 'forced_import_failure');
                END
                """.trimIndent()
            )

            val failure = runCatching {
                importer.importScenario(OWNER, scenario.id)
            }.exceptionOrNull()

            assertNotNull(failure)
            assertTrue(
                generateSequence(failure) { it.cause }.any {
                    it.message?.contains(
                        "forced_import_failure"
                    ) == true
                }
            )
            assertEquals(
                before,
                dao.observeProfile(OWNER).first()
            )
            assertEquals(
                catalogBefore,
                scenarios.observeCatalog().first()
            )
        }

    companion object {
        private const val OWNER = "demo@energy.test"
        private const val OTHER_OWNER = "other@energy.test"
    }
}