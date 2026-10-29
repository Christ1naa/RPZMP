package ua.kpi.energyforecast.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import ua.kpi.energyforecast.EnergyDashboard
import ua.kpi.energyforecast.ui.screens.ApplianceForecastScreen
import ua.kpi.energyforecast.ui.screens.ForecastScreen
import ua.kpi.energyforecast.ui.screens.MainMenuScreen

/**
 * Зіставляє маршрути з екранами.
 * Параметр days читається як Int і задає початковий період форми.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
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
                }
            )
        }

        composable(AppRoutes.APPLIANCES) {
            ApplianceForecastScreen()
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
                onCalculated = onForecastCalculated
            )
        }

        composable(AppRoutes.DEMO) {
            EnergyDashboard()
        }
    }
}