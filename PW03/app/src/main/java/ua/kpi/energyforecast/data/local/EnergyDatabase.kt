package ua.kpi.energyforecast.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        EnergyProfileEntity::class,
        EnergyDeviceEntity::class,
        ForecastRecordEntity::class,
        ForecastItemEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class EnergyDatabase : RoomDatabase() {

    abstract fun energyDao(): EnergyDao

    abstract fun forecastHistoryDao(): ForecastHistoryDao

    companion object {
        @Volatile
        private var instance: EnergyDatabase? = null

        /** Додає історію без видалення наявних профілів і приладів. */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS forecast_records (
                        id TEXT NOT NULL,
                        ownerKey TEXT NOT NULL,
                        title TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        forecastDays INTEGER NOT NULL,
                        dailyConsumptionKwh REAL NOT NULL,
                        periodConsumptionKwh REAL NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    index_forecast_records_ownerKey
                    ON forecast_records(ownerKey)
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS forecast_items (
                        forecastId TEXT NOT NULL,
                        position INTEGER NOT NULL,
                        deviceId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        powerWatts REAL NOT NULL,
                        hoursPerDay REAL NOT NULL,
                        deviceCount INTEGER NOT NULL,
                        PRIMARY KEY(forecastId, position),
                        FOREIGN KEY(forecastId)
                            REFERENCES forecast_records(id)
                            ON UPDATE NO ACTION
                            ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS
                    index_forecast_items_forecastId
                    ON forecast_items(forecastId)
                    """.trimIndent()
                )
            }
        }

        /** Використовує applicationContext і один екземпляр бази. */
        fun getInstance(context: Context): EnergyDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    EnergyDatabase::class.java,
                    "energy_forecast.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also {
                        instance = it
                    }
            }
        }
    }
}