package ua.kpi.energyforecast.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object ScenarioCacheMigration {

    /**
     * Додає таблиці кешу.
     * Наявні профілі, прилади та історія не змінюються.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS scenario_cache_meta (
                    id INTEGER NOT NULL,
                    updatedAtMillis INTEGER NOT NULL,
                    PRIMARY KEY(id)
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS cached_scenarios (
                    id TEXT NOT NULL,
                    catalogId INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    category TEXT NOT NULL,
                    description TEXT NOT NULL,
                    PRIMARY KEY(id),
                    FOREIGN KEY(catalogId)
                        REFERENCES scenario_cache_meta(id)
                        ON UPDATE NO ACTION
                        ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                index_cached_scenarios_catalogId
                ON cached_scenarios(catalogId)
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS cached_scenario_devices (
                    scenarioId TEXT NOT NULL,
                    position INTEGER NOT NULL,
                    deviceId TEXT NOT NULL,
                    name TEXT NOT NULL,
                    powerWatts REAL NOT NULL,
                    hoursPerDay REAL NOT NULL,
                    deviceCount INTEGER NOT NULL,
                    PRIMARY KEY(scenarioId, position),
                    FOREIGN KEY(scenarioId)
                        REFERENCES cached_scenarios(id)
                        ON UPDATE NO ACTION
                        ON DELETE CASCADE
                )
                """.trimIndent()
            )

            db.execSQL(
                """
                CREATE INDEX IF NOT EXISTS
                index_cached_scenario_devices_scenarioId
                ON cached_scenario_devices(scenarioId)
                """.trimIndent()
            )
        }
    }
}