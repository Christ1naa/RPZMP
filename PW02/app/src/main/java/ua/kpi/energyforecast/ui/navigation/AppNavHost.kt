package ua.kpi.energyforecast.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import ua.kpi.energyforecast.EnergyDashboard
import ua.kpi.energyforecast.model.EnergyDevice
import ua.kpi.energyforecast.ui.forecast.ForecastUiState
import ua.kpi.energyforecast.ui.screens.ApplianceForecastScreen
import ua.kpi.energyforecast.ui.screens.ForecastScreen
import ua.kpi.energyforecast.ui.screens.MainMenuScreen

/**
 * Зіставляє маршрути з екранами та передає стан і дії від власника ViewModel.
 * Аргумент days визначає початковий період форми швидкого прогнозу.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    forecastState: ForecastUiState,
    onDaysChange: (String) -> Unit,
    onSaveDevice: (EnergyDevice) -> Unit,
    onDeleteDevice: (String) -> Unit,
    lastForecast: String?,
    onForecastCalculated: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = AppRoutes.MENU,
        modifier = modifier
    ) {
        composable(AppRoutes.MENU) {
            MainMenuScreen(
                lastForecast = lastForecast,
                onAppliances = {
                    navController.navigate(AppRoutes.APPLIANCES) {
                        launchSingleTop = true
                    }
                },
                onQuickForecast = { days ->
                    navController.navigate(AppRoutes.quickForecast(days)) {
                        launchSingleTop = true
                    }
                },
                onDemo = {
                    navController.navigate(AppRoutes.DEMO) {
                        launchSingleTop = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(AppRoutes.APPLIANCES) {
            ApplianceForecastScreen(
                state = forecastState,
                onDaysChange = onDaysChange,
                onSaveDevice = onSaveDevice,
                onDeleteDevice = onDeleteDevice,
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(
            route = AppRoutes.QUICK_FORECAST,
            arguments = listOf(
                navArgument(AppRoutes.DAYS) {
                    type = NavType.IntType
                }
            )
        ) { entry ->
            val days = entry.arguments
                ?.getInt(AppRoutes.DAYS)
                ?.takeIf { it in 1..365 }
                ?: 30

            ForecastScreen(
                initialDays = days,
                onCalculated = onForecastCalculated,
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(AppRoutes.DEMO) {
            EnergyDashboard(
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}