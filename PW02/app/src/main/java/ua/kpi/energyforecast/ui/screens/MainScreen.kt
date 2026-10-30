package ua.kpi.energyforecast.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ua.kpi.energyforecast.model.DemoUser
import ua.kpi.energyforecast.ui.navigation.AppNavHost
import ua.kpi.energyforecast.ui.navigation.AppRoutes
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ua.kpi.energyforecast.ui.forecast.ForecastViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ua.kpi.energyforecast.data.RoomEnergyRepository
import ua.kpi.energyforecast.data.local.EnergyDatabase
import java.util.Locale

/**
 * Створює навігацію авторизованого користувача.
 * Підсумок швидкого прогнозу зберігається над NavHost і доступний після повернення.
 */
@Composable
fun MainScreen(
    user: DemoUser,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    // MainScreen викликається поза NavHost: ViewModel належить Activity.
// Окремий ключ не дозволяє змішувати прилади різних навчальних акаунтів.
    val applicationContext = LocalContext.current.applicationContext

    val ownerKey = remember(user.email) {
        user.email.trim().lowercase(Locale.ROOT)
    }

    val repository = remember(applicationContext) {
        RoomEnergyRepository(
            EnergyDatabase.getInstance(applicationContext)
        )
    }

    val forecastFactory = remember(repository, ownerKey) {
        viewModelFactory {
            initializer {
                ForecastViewModel(
                    repository = repository,
                    ownerKey = ownerKey
                )
            }
        }
    }

// Email залишається однаковим після повторного створення навчального акаунта.
    val forecastViewModel: ForecastViewModel = viewModel(
        key = "forecast_$ownerKey",
        factory = forecastFactory
    )

    val forecastState by forecastViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route

    var lastForecast by rememberSaveable {
        mutableStateOf<String?>(null)
    }

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
                style = MaterialTheme.typography.bodyLarge
            )

            TextButton(onClick = onLogout) {
                Text("Вийти")
            }
        }

        if (route != null && route != AppRoutes.MENU) {
            TextButton(
                onClick = { navController.popBackStack() }
            ) {
                Text("Назад до меню")
            }
        }

        AppNavHost(
            navController = navController,
            forecastState = forecastState,
            onDaysChange = forecastViewModel::updateDays,
            onSaveDevice = forecastViewModel::saveDevice,
            onDeleteDevice = forecastViewModel::deleteDevice,
            lastForecast = lastForecast,
            onForecastCalculated = { lastForecast = it },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
    }
}