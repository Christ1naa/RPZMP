package ua.kpi.energyforecast.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.kpi.energyforecast.ui.admin.AdminUiState

/** Показує останню отриману адміністративну статистику. */
@Composable
fun AdminScreen(
    state: AdminUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Адміністрування",
                style = MaterialTheme.typography.headlineMedium
            )

            Text("Статистика серверного каталогу сценаріїв.")

            Button(
                onClick = onRefresh,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (state.isLoading) {
                        "Завантаження…"
                    } else {
                        "Оновити статистику"
                    }
                )
            }

            if (state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            state.error?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error
                )
            }

            val summary = state.summary

            if (summary == null) {
                if (!state.isLoading) {
                    Text("Статистику ще не отримано.")
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Останні отримані показники",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            "Сценаріїв: ${summary.scenarioCount}"
                        )

                        Text(
                            "Позицій приладів: " +
                                    summary.devicePositionCount
                        )
                    }
                }

                Text(
                    "Кількість позицій — це кількість записів приладів " +
                            "у сценаріях, незалежно від кількості одиниць " +
                            "кожного приладу."
                )
            }
        }
    }
}