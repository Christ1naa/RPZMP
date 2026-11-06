package ua.kpi.energyforecast.data.local

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Об’єднує профіль, його прилади та добове споживання,
 * обчислене SQL-запитом DAO.
 */
data class EnergyProfileWithDevices(
    @Embedded
    val profile: EnergyProfileEntity,

    @Relation(
        parentColumn = "ownerKey",
        entityColumn = "ownerKey"
    )
    val devices: List<EnergyDeviceEntity>,

    val dailyConsumptionKwh: Double
)