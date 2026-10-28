package ua.kpi.energyforecast.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ua.kpi.energyforecast.EnergyDashboard
import ua.kpi.energyforecast.model.DemoUser

@Composable
fun MainScreen(
    user: DemoUser,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedScreen by rememberSaveable {
        mutableStateOf("forecast")
    }

    val screenStateHolder = rememberSaveableStateHolder()

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Вітаю, ${user.name}",
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge
            )

            TextButton(onClick = onLogout) {
                Text("Вийти")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            val screens = listOf(
                "demo" to "Демонстрація",
                "forecast" to "Мої прилади"
            )

            screens.forEach { (key, title) ->
                TextButton(
                    onClick = {
                        selectedScreen = key
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        color = if (selectedScreen == key) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            screenStateHolder.SaveableStateProvider(
                key = selectedScreen
            ) {
                when (selectedScreen) {
                    "demo" -> EnergyDashboard(
                        modifier = Modifier.fillMaxSize()
                    )

                    "forecast" -> ApplianceForecastScreen(
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}