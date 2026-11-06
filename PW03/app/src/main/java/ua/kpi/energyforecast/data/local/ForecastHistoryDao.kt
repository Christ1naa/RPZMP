package ua.kpi.energyforecast.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ForecastHistoryDao {

    @Insert
    abstract suspend fun insertRecord(
        record: ForecastRecordEntity
    )

    @Insert
    abstract suspend fun insertItems(
        items: List<ForecastItemEntity>
    )

    /**
     * Запис і всі його позиції зберігаються разом.
     * Помилка вставлення позиції скасовує також вставлення запису.
     */
    @Transaction
    open suspend fun saveForecast(
        record: ForecastRecordEntity,
        items: List<ForecastItemEntity>
    ) {
        require(items.isNotEmpty()) {
            "Прогноз має містити хоча б один прилад."
        }
        require(items.all { it.forecastId == record.id }) {
            "Позиції належать іншому прогнозу."
        }

        insertRecord(record)
        insertItems(items)
    }

    @Transaction
    @Query(
        """
        SELECT * FROM forecast_records
        WHERE ownerKey = :ownerKey
        ORDER BY createdAt DESC, id ASC
        """
    )
    abstract fun observeHistory(
        ownerKey: String
    ): Flow<List<ForecastWithItems>>

    @Transaction
    @Query(
        """
        SELECT * FROM forecast_records
        WHERE ownerKey = :ownerKey AND id = :forecastId
        """
    )
    abstract fun observeForecast(
        ownerKey: String,
        forecastId: String
    ): Flow<ForecastWithItems?>

    /** Зовнішній ключ автоматично видаляє позиції цього запису. */
    @Query(
        """
        DELETE FROM forecast_records
        WHERE ownerKey = :ownerKey AND id = :forecastId
        """
    )
    abstract suspend fun deleteForecast(
        ownerKey: String,
        forecastId: String
    )
}