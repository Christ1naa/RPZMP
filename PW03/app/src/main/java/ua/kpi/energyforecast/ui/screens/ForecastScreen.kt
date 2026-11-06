package ua.kpi.energyforecast.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ua.kpi.energyforecast.logic.ForecastValidator
import ua.kpi.energyforecast.model.ForecastResult
import ua.kpi.energyforecast.ui.components.ConsumptionChart
import java.util.Locale
import ua.kpi.energyforecast.ui.forecast.QuickForecastUiState

/**
 * Відображає стан швидкого прогнозу та передає введення у ViewModel.
 * Форматування підсумку для головного меню залишається відповідальністю UI.
 */
@Composable
fun ForecastScreen(
    state: QuickForecastUiState,
    onFieldChange: (String, String) -> Unit,
    onCalculate: () -> ForecastResult?,
    modifier: Modifier = Modifier,
    onCalculated: (String) -> Unit = {}
) {
    val errors = state.errors
    val result = state.result

    Column(
        modifier = modifier
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Розрахунок споживання",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Укажіть параметри одного типу приладів. " +
                    "Прогноз передбачає однаковий режим роботи щодня.",
            style = MaterialTheme.typography.bodyMedium
        )

        NumericInput(
            value = state.powerText,
            onValueChange = {
                onFieldChange(ForecastValidator.POWER, it)
            },
            label = "Потужність одного приладу, Вт",
            error = errors[ForecastValidator.POWER],
            keyboardType = KeyboardType.Decimal
        )

        NumericInput(
            value = state.hoursText,
            onValueChange = {
                onFieldChange(ForecastValidator.HOURS, it)
            },
            label = "Час роботи на добу, год",
            error = errors[ForecastValidator.HOURS],
            keyboardType = KeyboardType.Decimal
        )

        NumericInput(
            value = state.countText,
            onValueChange = {
                onFieldChange(ForecastValidator.COUNT, it)
            },
            label = "Кількість однакових приладів",
            error = errors[ForecastValidator.COUNT],
            keyboardType = KeyboardType.Number
        )

        NumericInput(
            value = state.daysText,
            onValueChange = {
                onFieldChange(ForecastValidator.DAYS, it)
            },
            label = "Період прогнозування, днів",
            error = errors[ForecastValidator.DAYS],
            keyboardType = KeyboardType.Number
        )

        Button(
            onClick = {
                onCalculate()?.let { calculated ->
                    onCalculated(
                        "За ${calculated.forecastDays} днів: " +
                                "${formatEnergy(calculated.periodConsumptionKwh)} кВт·год"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Розрахувати прогноз")
        }

        if (result != null) {
            ForecastResultCard(result)

            Text(
                text = "Накопичене споживання",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Вісь X — дні. Вісь Y — енергія, кВт·год.",
                style = MaterialTheme.typography.bodyMedium
            )

            ConsumptionChart(result = result)

            Text(
                text = "Пряма лінія відповідає припущенню про " +
                        "стале добове споживання. Це розрахункова " +
                        "оцінка, а не показання лічильника.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Показує числове поле з одиницями в підписі та помилкою зовнішньої валідації.
 */
@Composable
private fun NumericInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String?,
    keyboardType: KeyboardType
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = error != null,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType
        ),
        supportingText = {
            if (error != null) {
                Text(error)
            }
        }
    )
}

/**
 * Відображає добову й сумарну енергію у кВт·год.
 * Округлення до двох знаків виконується лише для тексту, а не в моделі розрахунку.
 */
@Composable
fun ForecastResultCard(
    result: ForecastResult
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Результат прогнозування",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "На добу: " +
                        "${formatEnergy(result.dailyConsumptionKwh)} кВт·год",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "За ${result.forecastDays} днів: " +
                        "${formatEnergy(result.periodConsumptionKwh)} кВт·год",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

private fun formatEnergy(value: Double): String {
    return String.format(
        Locale.forLanguageTag("uk-UA"),
        "%.2f",
        value
    )
}