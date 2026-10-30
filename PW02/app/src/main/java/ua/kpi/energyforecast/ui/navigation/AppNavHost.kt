package ua.kpi.energyforecast.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import ua.kpi.energyforecast.EnergyDashboard
import ua.kpi.energyforecast.model.EnergyDevice
import ua.kpi.energyforecast.ui.forecast.DeviceSort
import ua.kpi.energyforecast.ui.forecast.ForecastUiState
import ua.kpi.energyforecast.ui.forecast.QuickForecastViewModel
import ua.kpi.energyforecast.ui.screens.ApplianceForecastScreen
import ua.kpi.energyforecast.ui.screens.ForecastScreen
import ua.kpi.energyforecast.ui.screens.MainMenuScreen
import ua.kpi.energyforecast.ui.history.ForecastHistoryViewModel
import ua.kpi.energyforecast.ui.screens.ForecastHistoryScreen
import ua.kpi.energyforecast.ui.screens.ForecastDetailsScreen

/**
 * Зіставляє маршрути з екранами та передає стан і дії основного прогнозу.
 * Швидкий прогноз має окрему ViewModel, прив’язану до запису навігації.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    forecastState: ForecastUiState,
    onDaysChange: (String) -> Unit,
    onSaveDevice: (EnergyDevice) -> Unit,
    onDeleteDevice: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSortChange: (DeviceSort) -> Unit,
    lastForecast: String?,
    onForecastCalculated: (String) -> Unit,
    onRetryLoad: () -> Unit,
    onLoadDemoScenarios: (Boolean) -> Unit,
    onImportDemoScenario: (String) -> Unit,
    historyViewModel: ForecastHistoryViewModel,
    modifier: Modifier = Modifier
) {

    val historyState by historyViewModel.uiState.collectAsStateWithLifecycle()

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
                onHistory = {
                    navController.navigate(AppRoutes.HISTORY) {
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
                onSearchQueryChange = onSearchQueryChange,
                onSortChange = onSortChange,
                onRetryLoad = onRetryLoad,
                onLoadDemoScenarios = onLoadDemoScenarios,
                onImportDemoScenario = onImportDemoScenario,
                historyState = historyState,
                onSaveForecast = { title ->
                    val days = forecastState.forecastDays

                    if (
                        days != null &&
                        forecastState.result != null &&
                        !forecastState.isLoading &&
                        forecastState.loadError == null &&
                        !forecastState.isImportingDemo
                    ) {
                        historyViewModel.saveForecast(
                            title = title,
                            devices = forecastState.devices,
                            forecastDays = days
                        )
                    }
                },
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
            // Аргумент days надходить у SavedStateHandle цієї ViewModel.
            val quickViewModel: QuickForecastViewModel = viewModel(
                viewModelStoreOwner = entry
            )

            val quickState by quickViewModel.uiState
                .collectAsStateWithLifecycle()

            ForecastScreen(
                state = quickState,
                onFieldChange = quickViewModel::updateField,
                onCalculate = quickViewModel::calculate,
                onCalculated = onForecastCalculated,
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(AppRoutes.DEMO) {
            EnergyDashboard(
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(AppRoutes.HISTORY) {
            ForecastHistoryScreen(
                state = historyState,
                onSearchChange = historyViewModel::updateSearchQuery,
                onFilterChange = historyViewModel::updatePeriodFilter,
                onSortChange = historyViewModel::updateSort,
                onOpenForecast = { id ->
                    navController.navigate(AppRoutes.forecastDetails(id)) {
                        launchSingleTop = true
                    }
                },
                onDeleteForecast = historyViewModel::deleteForecast,
                onRetry = historyViewModel::retryLoad,
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(
            route = AppRoutes.FORECAST_DETAILS,
            arguments = listOf(
                navArgument(AppRoutes.FORECAST_ID) {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            val forecastId = requireNotNull(
                entry.arguments?.getString(AppRoutes.FORECAST_ID)
            )

            ForecastDetailsScreen(
                forecast = historyState.forecasts.firstOrNull {
                    it.id == forecastId
                },
                isLoading = historyState.isLoading,
                loadError = historyState.loadError,
                onRetry = historyViewModel::retryLoad,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}