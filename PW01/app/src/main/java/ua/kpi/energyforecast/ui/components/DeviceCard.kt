package ua.kpi.energyforecast.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.kpi.energyforecast.logic.EnergyCalculator
import ua.kpi.energyforecast.model.EnergyDevice
import java.util.Locale

@Composable
fun DeviceCard(
    device: EnergyDevice,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dailyConsumption =
        EnergyCalculator.calculateDeviceDaily(device)

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = device.name,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "${formatDeviceNumber(device.powerWatts)} Вт × " +
                        "${device.deviceCount} шт.",
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Робота на добу: " +
                        "${formatDeviceNumber(device.hoursPerDay)} год",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Споживання групи: " +
                        "${formatDeviceNumber(dailyConsumption)} кВт·год/добу",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Редагувати")
                }

                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Видалити",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun formatDeviceNumber(value: Double): String {
    return String.format(
        Locale.forLanguageTag("uk-UA"),
        "%.2f",
        value
    )
}