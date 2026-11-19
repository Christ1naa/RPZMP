package ua.kpi.energyforecast.pw05

import android.content.Intent
import android.os.Bundle
import android.hardware.SensorManager
import androidx.lifecycle.ViewModelProvider
import ua.kpi.energyforecast.pw05.data.*
import ua.kpi.energyforecast.pw05.ui.*
import ua.kpi.energyforecast.pw05.notifications.AlertCenter
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    private var detailId by mutableStateOf<Int?>(null)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        detailId = intent.getIntExtra("alert_id", -1).takeIf { it > 0 }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        detailId?.let { outState.putInt("detail_id", it) }
        super.onSaveInstanceState(outState)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        detailId = (savedInstanceState?.getInt("detail_id", -1) ?: intent.getIntExtra("alert_id", -1)).takeIf { it > 0 }
        val center = AlertCenter.get(this)
        center.restore()
        val preferences = SensorPreferences(applicationContext)
        val repository = SensorRepository(AndroidSensorBackend(getSystemService(SENSOR_SERVICE) as SensorManager))
        val sensors = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                SensorViewModel(repository, preferences.read(), preferences::write, center::sensor) as T
        })[SensorViewModel::class.java]
        val forecastPreferences = applicationContext.getSharedPreferences("forecast", 0)
        val forecast = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                ForecastViewModel(forecastPreferences.getInt("hours", 24).coerceIn(1, 8760)) {
                    forecastPreferences.edit().putInt("hours", it).apply()
                } as T
        })[ForecastViewModel::class.java]
        setContent {
            MaterialTheme {
                val model = forecast
                val dashboard: DashboardViewModel = viewModel()
                val state by model.state.collectAsStateWithLifecycle()
                val style by dashboard.state.collectAsStateWithLifecycle()
                // Один lifecycle-колектор для всіх панелей: компоненти не створюють дубльованих слухачів.
                val sensorState by sensors.state.collectAsStateWithLifecycle()
                // Сенсорні події не перебудовують незмінну 720-годинну енергетичну історію.
                val chartPoints = remember(style.period, style.generation) { dashboard.points(style) }
                var page by rememberSaveable { mutableIntStateOf(0) }
                LaunchedEffect(detailId) { if (detailId != null) page = 3 }
                Scaffold { padding ->
                    Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Прогноз споживання електроенергії", style = MaterialTheme.typography.headlineSmall)
                        Text("PW05 • Варіант 1")
                        listOf("Прогноз і графіки", "Сенсори", "Власні компоненти", "Сповіщення").forEachIndexed { index, name ->
                            OutlinedButton({ page = index }, Modifier.fillMaxWidth(), enabled = page != index) { Text(name) }
                        }
                        when (page) {
                            0 -> {
                                Text("Навчальний прогноз сталого середнього. Дані демонстраційні; це не виміри лічильника.")
                                Text("Горизонт: ${state.hours} год; прогноз: ${energy(state.forecastKwh)} кВт·год")
                                Button({ model.setHorizon(if (state.hours == 24) 168 else 24) }) { Text("Змінити горизонт: доба / тиждень") }
                                ChartPanel(style, chartPoints, dashboard::update)
                            }
                            1 -> SensorPanel(sensorState, sensors::select, sensors::threshold)
                            2 -> SensorVisuals(sensorState, style, dashboard::update)
                            3 -> {
                                Text("Актуальні сенсори: " + sensorState.snapshots.joinToString { "${it.kind.title}: ${it.status}, ${it.readings.lastOrNull()?.magnitude ?: "—"} ${it.kind.unit}" })
                                AlertPanel(center, detailId)
                            }
                        }
                    }
                }
            }
        }
    }
}

