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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ua.kpi.energyforecast.logic.EnergyCalculator
import ua.kpi.energyforecast.logic.ForecastValidator
import ua.kpi.energyforecast.model.ForecastResult
import ua.kpi.energyforecast.ui.components.ConsumptionChart
import java.util.Locale

/**
 * Форма прогнозування для одного типу електроприладів.
 * Кожна зміна параметрів приховує попередній результат.
 */
@Composable
fun ForecastScreen(
    modifier: Modifier = Modifier
) {
    var powerText by rememberSaveable { mutableStateOf("1000") }
    var hoursText by rememberSaveable { mutableStateOf("2") }
    var countText by rememberSaveable { mutableStateOf("1") }
    var daysText by rememberSaveable { mutableStateOf("30") }

    var showErrors by rememberSaveable { mutableStateOf(false) }
    var hasCalculated by rememberSaveable { mutableStateOf(false) }

    val validation = ForecastValidator.validate(
        powerText = powerText,
        hoursText = hoursText,
        countText = countText,
        daysText = daysText
    )

    val errors = if (showErrors) {
        validation.errors
    } else {
        emptyMap()
    }

    val result = if (hasCalculated) {
        validation.parameters?.let { parameters ->
            EnergyCalculator.calculate(parameters)
        }
    } else {
        null
    }

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
            value = powerText,
            onValueChange = {
                powerText = it
                hasCalculated = false
            },
            label = "Потужність одного приладу, Вт",
            error = errors[ForecastValidator.POWER],
            keyboardType = KeyboardType.Decimal
        )

        NumericInput(
            value = hoursText,
            onValueChange = {
                hoursText = it
                hasCalculated = false
            },
            label = "Час роботи на добу, год",
            error = errors[ForecastValidator.HOURS],
            keyboardType = KeyboardType.Decimal
        )

        NumericInput(
            value = countText,
            onValueChange = {
                countText = it
                hasCalculated = false
            },
            label = "Кількість однакових приладів",
            error = errors[ForecastValidator.COUNT],
            keyboardType = KeyboardType.Number
        )

        NumericInput(
            value = daysText,
            onValueChange = {
                daysText = it
                hasCalculated = false
            },
            label = "Період прогнозування, днів",
            error = errors[ForecastValidator.DAYS],
            keyboardType = KeyboardType.Number
        )

        Button(
            onClick = {
                showErrors = true
                hasCalculated = validation.parameters != null
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
 * Повторно використовуване числове поле з повідомленням про помилку.
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
 * Відображає готовий результат без виконання обчислень у картці.
 */
@Composable
private fun ForecastResultCard(
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