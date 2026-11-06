package ua.kpi.energyforecast.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Складений ключ дозволяє різним профілям мати незалежні списки.
 * Зовнішній ключ не допускає приладів без профілю власника.
 */
@Entity(
    tableName = "energy_devices",
    primaryKeys = ["ownerKey", "id"],
    foreignKeys = [
        ForeignKey(
            entity = EnergyProfileEntity::class,
            parentColumns = ["ownerKey"],
            childColumns = ["ownerKey"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["ownerKey"])
    ]
)
data class EnergyDeviceEntity(
    val ownerKey: String,
    val id: String,
    val name: String,
    val powerWatts: Double,
    val hoursPerDay: Double,
    val deviceCount: Int
)