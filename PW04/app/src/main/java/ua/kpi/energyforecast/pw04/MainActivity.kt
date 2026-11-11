package ua.kpi.energyforecast.pw04

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import ua.kpi.energyforecast.pw04.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MaterialTheme { EnergyApp() } }
    }
}

@Composable
fun EnergyApp(model: ForecastViewModel = viewModel()) {
    val state by model.uiState.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    Scaffold(bottomBar = {
        if (route?.startsWith("details") != true) NavigationBar {
            listOf("monitor" to "Моніторинг", "sites" to "Об'єкти й мапа").forEach { (target, label) ->
                NavigationBarItem(selected = route == target,
                    onClick = { nav.navigate(target) { popUpTo(nav.graph.startDestinationId) { saveState = true }; launchSingleTop = true; restoreState = true } },
                    icon = { Text(if (target == "monitor") "⚡" else "◎") }, label = { Text(label) })
            }
        }
    }) { padding ->
        NavHost(nav, startDestination = "monitor", modifier = Modifier.fillMaxSize().padding(padding)) {
            composable("monitor") {
                MonitorScreen(state.telemetry, model::simulateFailure, model::restart)
            }
            composable("sites") {
                SiteBrowser(state, model::updateQuery, model::select,
                    onDetails = { nav.navigate("details/$it") }) {
                    SiteMapPanel(state.visible, state.selectedId, model::select)
                }
            }
            composable("details/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) { detailEntry ->
                val id = detailEntry.arguments?.getString("id")
                SiteDetails(state.telemetry.readings.firstOrNull { it.site.id == id }, onBack = { nav.popBackStack() })
            }
        }
    }
}
