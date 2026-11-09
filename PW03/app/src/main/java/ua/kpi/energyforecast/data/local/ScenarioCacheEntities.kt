package ua.kpi.energyforecast.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * Один запис для всього каталогу.
 * Його наявність відрізняє завантажений порожній каталог
 * від ситуації, коли каталог ще не завантажували.
 */
@Entity(tableName = "scenario_cache_meta")
data class ScenarioCacheMetaEntity(
    @PrimaryKey
    val id: Int = 1,
    val updatedAtMillis: Long
)

@Entity(
    tableName = "cached_scenarios",
    foreignKeys = [
        ForeignKey(
            entity = ScenarioCacheMetaEntity::class,
            parentColumns = ["id"],
            childColumns = ["catalogId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["catalogId"])]
)
data class CachedScenarioEntity(
    @PrimaryKey
    val id: String,
    val catalogId: Int = 1,
    val name: String,
    val category: String,
    val description: String
)

@Entity(
    tableName = "cached_scenario_devices",
    primaryKeys = ["scenarioId", "position"],
    foreignKeys = [
        ForeignKey(
            entity = CachedScenarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["scenarioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["scenarioId"])]
)
data class CachedScenarioDeviceEntity(
    val scenarioId: String,
    val position: Int,
    val deviceId: String,
    val name: String,
    val powerWatts: Double,
    val hoursPerDay: Double,
    val deviceCount: Int
)

/** Сценарій із пов'язаними позиціями приладів. */
data class CachedScenarioWithDevices(
    @Embedded
    val scenario: CachedScenarioEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "scenarioId"
    )
    val devices: List<CachedScenarioDeviceEntity>
)

/** Каталог, його час оновлення та вкладені сценарії. */
data class CachedCatalogWithScenarios(
    @Embedded
    val metadata: ScenarioCacheMetaEntity,

    @Relation(
        entity = CachedScenarioEntity::class,
        parentColumn = "id",
        entityColumn = "catalogId"
    )
    val scenarios: List<CachedScenarioWithDevices>
)