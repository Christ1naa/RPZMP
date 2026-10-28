package ua.kpi.energyforecast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ua.kpi.energyforecast.ui.theme.EnergyForecastTheme
import java.util.Locale
import kotlin.random.Random

private data class DemoForecast(
    val dailyConsumptionKwh: Float,
    val activeDevices: Int
)

private fun generateDemoForecast(): DemoForecast {
    val deviceCount = Random.nextInt(from = 3, until = 11)

    val totalConsumption = (1..deviceCount).sumOf {
        Random.nextInt(from = 30, until = 201)
    } / 100f

    return DemoForecast(
        dailyConsumptionKwh = totalConsumption,
        activeDevices = deviceCount
    )
}

@Composable
fun EnergyDashboard(
    modifier: Modifier = Modifier
) {
    var dailyConsumption by rememberSaveable {
        mutableStateOf(8.4f)
    }

    var activeDevices by rememberSaveable {
        mutableStateOf(6)
    }

    var updateCount by rememberSaveable {
        mutableStateOf(0)
    }

    val ukrainianLocale = remember {
        Locale.forLanguageTag("uk-UA")
    }

    val animatedConsumption by animateFloatAsState(
        targetValue = dailyConsumption,
        animationSpec = tween(durationMillis = 900),
        label = "dailyConsumptionAnimation"
    )

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        DashboardHeader()

        ConsumptionCard(
            consumptionText = String.format(
                ukrainianLocale,
                "%.2f",
                animatedConsumption
            )
        )

        DeviceSummary(
            activeDevices = activeDevices,
            updateCount = updateCount
        )

        Button(
            onClick = {
                val forecast = generateDemoForecast()

                dailyConsumption = forecast.dailyConsumptionKwh
                activeDevices = forecast.activeDevices
                updateCount += 1
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Оновити демодані")
        }

        DemoInformation()
    }
}

@Composable
private fun DashboardHeader() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "EnergyForecast",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Система прогнозування споживання електроенергії",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun ConsumptionCard(
    consumptionText: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Прогноз на добу",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = consumptionText,
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "кВт·год",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun DeviceSummary(
    activeDevices: Int,
    updateCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Активні прилади",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    text = activeDevices.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Кількість оновлень: $updateCount",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DemoInformation() {
    Text(
        text = "Демонстраційний режим. Показники генеруються " +
                "випадково для перевірки інтерфейсу. Розрахунок за " +
                "параметрами електроприладів буде додано на наступному етапі.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Preview(
    name = "Світла тема",
    showBackground = true
)
@Composable
private fun EnergyDashboardPreview() {
    EnergyForecastTheme(
        darkTheme = false,
        dynamicColor = false
    ) {
        Scaffold { innerPadding ->
            EnergyDashboard(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

@Preview(
    name = "Темна тема",
    showBackground = true
)
@Composable
private fun EnergyDashboardDarkPreview() {
    EnergyForecastTheme(
        darkTheme = true,
        dynamicColor = false
    ) {
        Scaffold { innerPadding ->
            EnergyDashboard(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}