package ua.kpi.energyforecast.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Відокремлює списки різних власників і зберігає період прогнозу.
 * ownerKey має залишатися стабільним між запусками застосунку.
 */
@Entity(tableName = "energy_profiles")
data class EnergyProfileEntity(
    @PrimaryKey
    val ownerKey: String,
    val forecastDays: Int = 30
)