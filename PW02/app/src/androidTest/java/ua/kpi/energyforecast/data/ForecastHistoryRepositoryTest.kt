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
import ua.kpi.energyforecast.data.local.toEntity
import ua.kpi.energyforecast.model.EnergyDevice

@RunWith(AndroidJUnit4::class)
class ForecastHistoryRepositoryTest {

    private lateinit var database: EnergyDatabase
    private lateinit var repository: RoomForecastHistoryRepository

    @Before
    fun setUp() {
        val context: Context =
            InstrumentationRegistry.getInstrumentation().targetContext

        database = Room.inMemoryDatabaseBuilder(
            context,
            EnergyDatabase::class.java
        ).build()

        repository = RoomForecastHistoryRepository(
            database = database,
            now = { 1_000L }
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun savedForecastKeepsOriginalParametersAndResult() = runBlocking {
        val original = device()

        database.energyDao().saveDevice(original.toEntity(OWNER))

        val id = repository.saveForecast(
            ownerKey = OWNER,
            title = "Контрольний прогноз",
            devices = listOf(original),
            forecastDays = 30
        )

        database.energyDao().saveDevice(
            original.copy(
                name = "Змінений прилад",
                powerWatts = 2000.0
            ).toEntity(OWNER)
        )
        database.energyDao().deleteDevice(OWNER, original.id)

        val saved = requireNotNull(
            repository.observeForecast(OWNER, id).first()
        )

        assertEquals("Контрольний прогноз", saved.title)
        assertEquals(1_000L, saved.createdAt)
        assertEquals(listOf(original), saved.devices)
        assertEquals(2.0, saved.result.dailyConsumptionKwh, 0.000001)
        assertEquals(60.0, saved.result.periodConsumptionKwh, 0.000001)
    }

    @Test
    fun anotherOwnerCannotReadOrDeleteForecast() = runBlocking {
        val id = repository.saveForecast(
            ownerKey = OWNER,
            title = "Мій прогноз",
            devices = listOf(device()),
            forecastDays = 7
        )

        assertTrue(repository.observeHistory(OTHER_OWNER).first().isEmpty())
        assertNull(repository.observeForecast(OTHER_OWNER, id).first())

        repository.deleteForecast(OTHER_OWNER, id)

        assertNotNull(repository.observeForecast(OWNER, id).first())
    }

    @Test
    fun deletingForecastAlsoDeletesItsItems() = runBlocking {
        val id = repository.saveForecast(
            ownerKey = OWNER,
            title = "Для видалення",
            devices = listOf(device()),
            forecastDays = 7
        )

        repository.deleteForecast(OWNER, id)

        assertNull(repository.observeForecast(OWNER, id).first())

        database.openHelper.readableDatabase.query(
            "SELECT COUNT(*) FROM forecast_items"
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun failedItemInsertRollsBackWholeForecast() = runBlocking {
        // Штучна помилка всередині транзакції перевіряє її атомарність.
        database.openHelper.writableDatabase.execSQL(
            """
            CREATE TRIGGER reject_forecast_item
            BEFORE INSERT ON forecast_items
            BEGIN
                SELECT RAISE(ABORT, 'Test insert failure');
            END
            """.trimIndent()
        )

        val failure = runCatching {
            repository.saveForecast(
                ownerKey = OWNER,
                title = "Невдале збереження",
                devices = listOf(device()),
                forecastDays = 7
            )
        }.exceptionOrNull()

        assertNotNull(failure)
        assertTrue(repository.observeHistory(OWNER).first().isEmpty())
    }

    private fun device() = EnergyDevice(
        id = "device-1",
        name = "Обігрівач",
        powerWatts = 1000.0,
        hoursPerDay = 2.0,
        deviceCount = 1
    )

    companion object {
        private const val OWNER = "demo@energy.test"
        private const val OTHER_OWNER = "other@energy.test"
    }
}