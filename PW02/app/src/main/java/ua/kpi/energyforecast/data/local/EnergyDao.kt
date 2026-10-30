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
     * Повертає профіль, його прилади та добове споживання,
     * обчислене засобами SQL.
     *
     * Потужність переводиться з ватів у кіловати. COALESCE повертає
     * нуль для профілю без приладів. Підсумок обмежений ownerKey.
     *
     * Транзакція забезпечує узгоджене читання профілю,
     * розрахованого значення та пов’язаного списку приладів.
     */
    @Transaction
    @Query(
        """
        SELECT energy_profiles.*,
               (
                   SELECT COALESCE(
                       SUM(
                           energy_devices.powerWatts / 1000.0 *
                           energy_devices.hoursPerDay *
                           energy_devices.deviceCount
                       ),
                       0.0
                   )
                   FROM energy_devices
                   WHERE energy_devices.ownerKey =
                         energy_profiles.ownerKey
               ) AS dailyConsumptionKwh
        FROM energy_profiles
        WHERE energy_profiles.ownerKey = :ownerKey
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
     * Створює профіль перед збереженням приладу.
     * IGNORE зберігає період прогнозу наявного профілю.
     * Обидві операції виконуються в одній транзакції.
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

    /**
     * Зберігає допустимий період прогнозу.
     * Якщо профілю ще немає, створює його в цій самій транзакції.
     */
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
}