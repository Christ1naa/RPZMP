package ua.kpi.energyforecast.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ScenarioCacheDao {

    /**
     * Метадані та вкладені списки читаються узгоджено.
     * Flow повідомляє про зміни кешу.
     */
    @Transaction
    @Query("SELECT * FROM scenario_cache_meta WHERE id = 1")
    fun observeCatalog(): Flow<CachedCatalogWithScenarios?>

    @Query("DELETE FROM scenario_cache_meta")
    suspend fun deleteCatalog()

    @Insert
    suspend fun insertMetadata(metadata: ScenarioCacheMetaEntity)

    @Insert
    suspend fun insertScenarios(scenarios: List<CachedScenarioEntity>)

    @Insert
    suspend fun insertDevices(devices: List<CachedScenarioDeviceEntity>)
}