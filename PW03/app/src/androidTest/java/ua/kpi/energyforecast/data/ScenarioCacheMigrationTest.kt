package ua.kpi.energyforecast.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ua.kpi.energyforecast.data.local.EnergyDatabase
import ua.kpi.energyforecast.data.local.ScenarioCacheMigration

@RunWith(AndroidJUnit4::class)
class ScenarioCacheMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EnergyDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migration2To3PreservesProfileDevicesAndHistory() {
        helper.createDatabase(DATABASE_2_3, 2).apply {
            insertProfileAndDevice()

            execSQL(
                """
                INSERT INTO forecast_records (
                    id,
                    ownerKey,
                    title,
                    createdAt,
                    forecastDays,
                    dailyConsumptionKwh,
                    periodConsumptionKwh
                ) VALUES (
                    'forecast-1',
                    'demo@energy.test',
                    'Перевірка міграції',
                    1000,
                    30,
                    0.48,
                    14.4
                )
                """.trimIndent()
            )

            execSQL(
                """
                INSERT INTO forecast_items (
                    forecastId,
                    position,
                    deviceId,
                    name,
                    powerWatts,
                    hoursPerDay,
                    deviceCount
                ) VALUES (
                    'forecast-1',
                    0,
                    'device-1',
                    'Ноутбук',
                    60.0,
                    8.0,
                    1
                )
                """.trimIndent()
            )

            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            DATABASE_2_3,
            3,
            true,
            ScenarioCacheMigration.MIGRATION_2_3
        )

        migrated.use { db ->
            assertProfileAndDevice(db)

            db.query(
                """
                SELECT title, forecastDays, periodConsumptionKwh
                FROM forecast_records
                WHERE id = 'forecast-1'
                """.trimIndent()
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("Перевірка міграції", cursor.getString(0))
                assertEquals(30, cursor.getInt(1))
                assertEquals(14.4, cursor.getDouble(2), 0.000001)
            }

            db.query(
                """
                SELECT deviceId, powerWatts, hoursPerDay, deviceCount
                FROM forecast_items
                WHERE forecastId = 'forecast-1' AND position = 0
                """.trimIndent()
            ).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("device-1", cursor.getString(0))
                assertEquals(60.0, cursor.getDouble(1), 0.0)
                assertEquals(8.0, cursor.getDouble(2), 0.0)
                assertEquals(1, cursor.getInt(3))
            }

            assertTableEmpty(db, "scenario_cache_meta")
            assertTableEmpty(db, "cached_scenarios")
            assertTableEmpty(db, "cached_scenario_devices")
        }
    }

    @Test
    fun migration1To3PreservesExistingProfileAndDevice() {
        helper.createDatabase(DATABASE_1_3, 1).apply {
            insertProfileAndDevice()
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            DATABASE_1_3,
            3,
            true,
            EnergyDatabase.MIGRATION_1_2,
            ScenarioCacheMigration.MIGRATION_2_3
        )

        migrated.use { db ->
            assertProfileAndDevice(db)

            assertTableEmpty(db, "forecast_records")
            assertTableEmpty(db, "forecast_items")
            assertTableEmpty(db, "scenario_cache_meta")
            assertTableEmpty(db, "cached_scenarios")
            assertTableEmpty(db, "cached_scenario_devices")
        }
    }

    private fun SupportSQLiteDatabase.insertProfileAndDevice() {
        execSQL(
            """
            INSERT INTO energy_profiles (ownerKey, forecastDays)
            VALUES ('demo@energy.test', 30)
            """.trimIndent()
        )

        execSQL(
            """
            INSERT INTO energy_devices (
                ownerKey,
                id,
                name,
                powerWatts,
                hoursPerDay,
                deviceCount
            ) VALUES (
                'demo@energy.test',
                'device-1',
                'Ноутбук',
                60.0,
                8.0,
                1
            )
            """.trimIndent()
        )
    }

    private fun assertProfileAndDevice(db: SupportSQLiteDatabase) {
        db.query(
            """
            SELECT forecastDays
            FROM energy_profiles
            WHERE ownerKey = 'demo@energy.test'
            """.trimIndent()
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(30, cursor.getInt(0))
        }

        db.query(
            """
            SELECT name, powerWatts, hoursPerDay, deviceCount
            FROM energy_devices
            WHERE ownerKey = 'demo@energy.test' AND id = 'device-1'
            """.trimIndent()
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Ноутбук", cursor.getString(0))
            assertEquals(60.0, cursor.getDouble(1), 0.0)
            assertEquals(8.0, cursor.getDouble(2), 0.0)
            assertEquals(1, cursor.getInt(3))
        }
    }

    private fun assertTableEmpty(
        db: SupportSQLiteDatabase,
        table: String
    ) {
        // Назви таблиць задаються лише константами всередині тесту.
        db.query("SELECT COUNT(*) FROM $table").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
    }

    companion object {
        private const val DATABASE_2_3 = "scenario_migration_2_3_test.db"
        private const val DATABASE_1_3 = "scenario_migration_1_3_test.db"
    }
}