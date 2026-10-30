package ua.kpi.energyforecast.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        EnergyProfileEntity::class,
        EnergyDeviceEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class EnergyDatabase : RoomDatabase() {

    abstract fun energyDao(): EnergyDao

    companion object {
        @Volatile
        private var instance: EnergyDatabase? = null

        /**
         * Єдиний екземпляр використовує applicationContext,
         * щоб база не утримувала Activity після її пересоздання.
         */
        fun getInstance(context: Context): EnergyDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    EnergyDatabase::class.java,
                    "energy_forecast.db"
                ).build().also {
                    instance = it
                }
            }
        }
    }
}