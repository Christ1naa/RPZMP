package ua.kpi.energyforecast.data.local

import androidx.room.Embedded
import androidx.room.Relation

/** Об’єднує профіль і його прилади для узгодженого читання через DAO. */
data class EnergyProfileWithDevices(
    @Embedded
    val profile: EnergyProfileEntity,

    @Relation(
        parentColumn = "ownerKey",
        entityColumn = "ownerKey"
    )
    val devices: List<EnergyDeviceEntity>
)