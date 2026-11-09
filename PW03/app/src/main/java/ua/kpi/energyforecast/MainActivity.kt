package ua.kpi.energyforecast

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import ua.kpi.energyforecast.ui.auth.ApiAuthViewModel
import ua.kpi.energyforecast.ui.screens.ApiLoginScreen
import ua.kpi.energyforecast.ui.screens.MainScreen
import ua.kpi.energyforecast.ui.theme.EnergyForecastTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            EnergyForecastTheme {
                val authViewModel: ApiAuthViewModel = viewModel()

                val sessionState by authViewModel.sessionState
                    .collectAsStateWithLifecycle()

                val formState by authViewModel.formState
                    .collectAsStateWithLifecycle()

                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner, authViewModel) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            authViewModel.checkExpiry()
                        }
                    }

                    lifecycleOwner.lifecycle.addObserver(observer)

                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    val contentModifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)

                    val session = sessionState.session

                    when {
                        !sessionState.isReady -> {
                            Box(
                                modifier = contentModifier,
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }

                        session == null -> {
                            ApiLoginScreen(
                                state = formState,
                                sessionMessage = sessionState.message,
                                onEmailChange = authViewModel::updateEmail,
                                onPasswordChange = authViewModel::updatePassword,
                                onSubmit = authViewModel::submit,
                                onPermissionDenied =
                                    authViewModel::permissionDenied,
                                modifier = contentModifier
                            )
                        }

                        else -> {
                            // Новий вхід створює окремий стан навігації.
                            key(session.sessionKey) {
                                CompositionLocalProvider(
                                    LocalViewModelStoreOwner provides
                                            authViewModel.sessionContentOwner
                                ) {
                                    Column(modifier = contentModifier) {
                                        formState.error?.let { message ->
                                            Text(
                                                text = message,
                                                color = MaterialTheme
                                                    .colorScheme.error,
                                                modifier = Modifier
                                                    .padding(16.dp)
                                            )
                                        }

                                        MainScreen(
                                            user = session.user,
                                            role = session.role,
                                            sessionRepository =
                                                authViewModel.repository,
                                            onLogout = authViewModel::logout,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
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
}