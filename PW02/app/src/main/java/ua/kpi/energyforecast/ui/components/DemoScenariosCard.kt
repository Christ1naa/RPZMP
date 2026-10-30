package ua.kpi.energyforecast.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.kpi.energyforecast.ui.forecast.ForecastUiState

/** Показує приклади та передає запити завантаження й імпорту у ViewModel. */
@Composable
fun DemoScenariosCard(
    state: ForecastUiState,
    onLoad: (Boolean) -> Unit,
    onImport: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val busy = state.isLoadingDemos || state.isImportingDemo

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Приклади наборів приладів",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Імпорт додає прилади до вашого списку. " +
                        "Поточний період прогнозування зберігається.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedButton(
                onClick = {
                    onLoad(state.demoScenarios.isNotEmpty())
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (state.demoScenarios.isEmpty()) {
                        "Завантажити приклади"
                    } else {
                        "Оновити приклади"
                    }
                )
            }

            if (busy) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = if (state.isImportingDemo) {
                        "Додавання приладів…"
                    } else {
                        "Завантаження прикладів…"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }

            state.demoError?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            state.demoMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            state.demoScenarios.forEach { scenario ->
                val fits = state.devices.size + scenario.devices.size <=
                        ForecastUiState.MAX_DEVICE_POSITIONS

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = scenario.name,
                        style = MaterialTheme.typography.titleSmall
                    )

                    Text(
                        text = scenario.devices.joinToString { it.name },
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "Позицій у наборі: ${scenario.devices.size}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    TextButton(
                        onClick = { onImport(scenario.id) },
                        enabled = !busy &&
                                !state.isLoading &&
                                state.loadError == null &&
                                fits
                    ) {
                        Text("Додати до моїх приладів")
                    }

                    if (!fits) {
                        Text(
                            text = "Недостатньо місця: дозволено до " +
                                    "${ForecastUiState.MAX_DEVICE_POSITIONS} позицій.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}