package ua.kpi.energyforecast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import ua.kpi.energyforecast.ui.auth.AuthViewModel
import ua.kpi.energyforecast.ui.screens.AuthScreen
import ua.kpi.energyforecast.ui.screens.MainScreen
import ua.kpi.energyforecast.ui.theme.EnergyForecastTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            EnergyForecastTheme {
                val authViewModel: AuthViewModel = viewModel()
                val authState = authViewModel.uiState

                // Ключ user.id відокремлює стани акаунтів і дозволяє повернутися до них після logout.
                // Saved UI state не замінює базу даних; акаунти ViewModel не переживають process death.
                val userStateHolder = rememberSaveableStateHolder()

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    val contentModifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)

                    val user = authState.user

                    if (user == null) {
                        AuthScreen(
                            state = authState,
                            onFieldChange = authViewModel::updateField,
                            onSubmit = authViewModel::submit,
                            onSwitchMode = authViewModel::switchMode,
                            modifier = contentModifier
                        )
                    } else {
                        userStateHolder.SaveableStateProvider(
                            key = user.id
                        ) {
                            MainScreen(
                                user = user,
                                onLogout = authViewModel::logout,
                                modifier = contentModifier
                            )
                        }
                    }
                }
            }
        }
    }
}