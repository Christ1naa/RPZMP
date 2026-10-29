package ua.kpi.energyforecast.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Показує доступні розділи та останній виконаний швидкий прогноз.
 * На широкому екрані обмежує ширину вмісту, на вузькому дозволяє прокручування.
 */
@Composable
fun MainMenuScreen(
    lastForecast: String?,
    onAppliances: () -> Unit,
    onQuickForecast: (Int) -> Unit,
    onDemo: () -> Unit,
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
                text = "Головне меню",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = "Оберіть спосіб прогнозування споживання електроенергії."
            )

            MenuSection(
                title = "Мої прилади",
                description = "Список електроприладів і загальний прогноз.",
                buttonText = "Відкрити прилади",
                onClick = onAppliances
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Швидкий прогноз",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        text = "Розрахунок для одного типу приладів. " +
                                "Період передається на наступний екран."
                    )

                    Button(
                        onClick = { onQuickForecast(7) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Прогноз на 7 днів")
                    }

                    OutlinedButton(
                        onClick = { onQuickForecast(30) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Прогноз на 30 днів")
                    }
                }
            }

            MenuSection(
                title = "Демонстрація",
                description = "Демонстраційний екран із практичної роботи №1.",
                buttonText = "Відкрити демонстрацію",
                onClick = onDemo
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Останній швидкий прогноз",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = lastForecast
                            ?: "Виконайте розрахунок і поверніться до меню."
                    )
                }
            }
        }
    }
}

/** Повторно використовувана картка переходу до розділу. */
@Composable
private fun MenuSection(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(text = description)
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(buttonText)
            }
        }
    }
}