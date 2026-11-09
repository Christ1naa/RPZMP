package ua.kpi.energyforecast.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ua.kpi.energyforecast.model.AuthField
import ua.kpi.energyforecast.ui.auth.ApiLoginUiState
import ua.kpi.energyforecast.ui.rememberLocalNetworkAction

@Composable
fun ApiLoginScreen(
    state: ApiLoginUiState,
    sessionMessage: String?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onPermissionDenied: () -> Unit,
    modifier: Modifier = Modifier
) {
    val submitWithPermission = rememberLocalNetworkAction(
        onAllowed = onSubmit,
        onDenied = onPermissionDenied
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "EnergyForecast",
                style = MaterialTheme.typography.headlineLarge
            )

            Text(
                "Вхід",
                style = MaterialTheme.typography.headlineMedium
            )

            Text("Увійдіть, щоб працювати зі своїми приладами та каталогом.")

            sessionMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            OutlinedTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                singleLine = true,
                enabled = !state.isSubmitting,
                isError = state.errors.containsKey(AuthField.EMAIL),
                supportingText = {
                    state.errors[AuthField.EMAIL]?.let {
                        Text(it)
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = { Text("Пароль") },
                singleLine = true,
                enabled = !state.isSubmitting,
                isError = state.errors.containsKey(AuthField.PASSWORD),
                supportingText = {
                    state.errors[AuthField.PASSWORD]?.let {
                        Text(it)
                    }
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                modifier = Modifier.fillMaxWidth()
            )

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = submitWithPermission,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (state.isSubmitting) {
                        "Вхід…"
                    } else {
                        "Увійти"
                    }
                )
            }

            if (state.isSubmitting) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}