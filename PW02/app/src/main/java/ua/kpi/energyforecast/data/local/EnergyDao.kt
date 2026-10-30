package ua.kpi.energyforecast.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class EnergyDao {

    /**
     * Room читає профіль і пов’язаний список в одній транзакції,
     * щоб спостерігач отримував узгоджений набір даних.
     */
    @Transaction
    @Query(
        """
        SELECT * FROM energy_profiles
        WHERE ownerKey = :ownerKey
        """
    )
    abstract fun observeProfile(
        ownerKey: String
    ): Flow<EnergyProfileWithDevices?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertProfile(
        profile: EnergyProfileEntity
    )

    @Upsert
    abstract suspend fun upsertDevice(
        device: EnergyDeviceEntity
    )

    /**
     * Профіль створюється перед приладом.
     * IGNORE зберігає раніше встановлений період прогнозу.
     */
    @Transaction
    open suspend fun saveDevice(device: EnergyDeviceEntity) {
        insertProfile(
            EnergyProfileEntity(ownerKey = device.ownerKey)
        )
        upsertDevice(device)
    }

    @Query(
        """
        DELETE FROM energy_devices
        WHERE ownerKey = :ownerKey AND id = :deviceId
        """
    )
    abstract suspend fun deleteDevice(
        ownerKey: String,
        deviceId: String
    )

    @Query(
        """
        UPDATE energy_profiles
        SET forecastDays = :days
        WHERE ownerKey = :ownerKey
        """
    )
    abstract suspend fun updateForecastDays(
        ownerKey: String,
        days: Int
    )

    @Transaction
    open suspend fun saveForecastDays(
        ownerKey: String,
        days: Int
    ) {
        require(days in 1..365) {
            "Період прогнозу має бути від 1 до 365 днів."
        }

        insertProfile(
            EnergyProfileEntity(ownerKey = ownerKey)
        )
        updateForecastDays(ownerKey, days)
    }

    /**
     * Пошук обмежений профілем власника.
     * Сортування за споживанням використовує розрахунковий SQL-вираз.
     */
    @Query(
        """
        SELECT * FROM energy_devices
        WHERE ownerKey = :ownerKey
          AND (
              :search = ''
              OR instr(lower(name), lower(:search)) > 0
          )
        ORDER BY
            CASE WHEN :sortByConsumption = 1
                THEN powerWatts * hoursPerDay * deviceCount
            END DESC,
            name COLLATE NOCASE ASC,
            id ASC
        """
    )
    abstract fun observeDevices(
        ownerKey: String,
        search: String,
        sortByConsumption: Boolean
    ): Flow<List<EnergyDeviceEntity>>
}