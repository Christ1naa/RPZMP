package ua.kpi.energyforecast.data.local

import ua.kpi.energyforecast.model.EnergyDevice

/** Перетворення відділяють модель розрахунків від структури таблиці Room. */
fun EnergyDeviceEntity.toModel(): EnergyDevice {
    return EnergyDevice(
        id = id,
        name = name,
        powerWatts = powerWatts,
        hoursPerDay = hoursPerDay,
        deviceCount = deviceCount
    )
}

fun EnergyDevice.toEntity(ownerKey: String): EnergyDeviceEntity {
    return EnergyDeviceEntity(
        ownerKey = ownerKey,
        id = id,
        name = name,
        powerWatts = powerWatts,
        hoursPerDay = hoursPerDay,
        deviceCount = deviceCount
    )
}