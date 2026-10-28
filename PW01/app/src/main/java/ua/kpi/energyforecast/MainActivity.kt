package ua.kpi.energyforecast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ua.kpi.energyforecast.ui.screens.ForecastScreen
import ua.kpi.energyforecast.ui.theme.EnergyForecastTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            EnergyForecastTheme {
                var selectedScreen by rememberSaveable {
                    mutableStateOf("forecast")
                }

                // Зберігає стан кожного екрана при перемиканні.
                val screenStateHolder = rememberSaveableStateHolder()

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(
                                onClick = {
                                    selectedScreen = "demo"
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Демонстрація",
                                    color = if (selectedScreen == "demo") {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }

                            TextButton(
                                onClick = {
                                    selectedScreen = "forecast"
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "Розрахунок",
                                    color = if (selectedScreen == "forecast") {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
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

                                    "forecast" -> ForecastScreen(
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}