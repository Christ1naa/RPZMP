package ua.kpi.energyforecast.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Запитує назву та передає підтверджене збереження у ViewModel. */
@Composable
fun SaveForecastCard(
    enabled: Boolean,
    isSaving: Boolean,
    message: String?,
    error: String?,
    onSave: (String) -> Unit
) {
    var dialogOpen by rememberSaveable { mutableStateOf(false) }
    var title by rememberSaveable { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Збереження прогнозу",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Збережіть результат і параметри всіх приладів, " +
                        "щоб переглянути їх пізніше."
            )

            Button(
                onClick = {
                    title = ""
                    dialogOpen = true
                },
                enabled = enabled && !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isSaving) "Збереження…" else "Зберегти прогноз")
            }

            if (isSaving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (dialogOpen) {
        val validTitle = title.trim().length in 1..60

        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            title = { Text("Назва прогнозу") },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Наприклад: Квартира, жовтень") },
                    singleLine = true,
                    isError = title.isNotEmpty() && !validTitle,
                    supportingText = { Text("Від 1 до 60 символів") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = validTitle && enabled && !isSaving,
                    onClick = {
                        onSave(title.trim())
                        dialogOpen = false
                    }
                ) {
                    Text("Зберегти")
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogOpen = false }) {
                    Text("Скасувати")
                }
            }
        )
    }
}