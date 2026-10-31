package ua.kpi.doctrack.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ua.kpi.doctrack.R
import ua.kpi.doctrack.ui.screens.DeadlinesScreen
import ua.kpi.doctrack.ui.screens.RecordsScreen
import ua.kpi.doctrack.ui.screens.ServerScreen
import androidx.compose.runtime.remember
import ua.kpi.doctrack.data.demo.createDemoRecords

private enum class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int
) {
    RECORDS("records", R.string.nav_records),
    DEADLINES("deadlines", R.string.nav_deadlines),
    SERVER("server", R.string.nav_server)
}

@Composable
fun DocTrackApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val demoRecords = remember {
        createDemoRecords()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val label = stringResource(destination.labelRes)

                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(
                                    navController.graph.findStartDestination().id
                                ) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Text(text = label.take(1))
                        },
                        label = {
                            Text(text = label)
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.RECORDS.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(TopLevelDestination.RECORDS.route) {
                RecordsScreen(records = demoRecords)
            }

            composable(TopLevelDestination.DEADLINES.route) {
                DeadlinesScreen()
            }

            composable(TopLevelDestination.SERVER.route) {
                ServerScreen()
            }
        }
    }
}