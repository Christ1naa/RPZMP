package ua.kpi.energyforecast.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * Незалежний запис історії.
 * ownerKey відокремлює історію навчальних профілів.
 */
@Entity(
    tableName = "forecast_records",
    indices = [Index(value = ["ownerKey"])]
)
data class ForecastRecordEntity(
    @PrimaryKey
    val id: String,
    val ownerKey: String,
    val title: String,
    val createdAt: Long,
    val forecastDays: Int,
    val dailyConsumptionKwh: Double,
    val periodConsumptionKwh: Double
)

/**
 * Копія приладу на момент збереження прогнозу.
 * Зв’язку з поточним energy_devices немає, тому його зміни не змінюють історію.
 */
@Entity(
    tableName = "forecast_items",
    primaryKeys = ["forecastId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = ForecastRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["forecastId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["forecastId"])]
)
data class ForecastItemEntity(
    val forecastId: String,
    val position: Int,
    val deviceId: String,
    val name: String,
    val powerWatts: Double,
    val hoursPerDay: Double,
    val deviceCount: Int
)

/** Узгоджений результат читання запису та всіх його позицій. */
data class ForecastWithItems(
    @Embedded
    val record: ForecastRecordEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "forecastId"
    )
    val items: List<ForecastItemEntity>
)