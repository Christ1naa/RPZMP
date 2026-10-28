package ua.kpi.energyforecast.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ua.kpi.energyforecast.R
import ua.kpi.energyforecast.data.DemoAuthRepository
import ua.kpi.energyforecast.model.AuthField
import ua.kpi.energyforecast.model.AuthMode
import ua.kpi.energyforecast.model.AuthUiState
import ua.kpi.energyforecast.ui.components.AuthPasswordField
import ua.kpi.energyforecast.ui.components.AuthTextField

/**
 * Відображає вхід або реєстрацію зі стану AuthViewModel та передає події назовні.
 * Під час обробки блокує поля й кнопки; прокручування та imePadding враховують клавіатуру.
 */
@Composable
fun AuthScreen(
    state: AuthUiState,
    onFieldChange: (AuthField, String) -> Unit,
    onSubmit: () -> Unit,
    onSwitchMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRegistration = state.mode == AuthMode.REGISTER
    val fieldsEnabled = !state.isLoading

    Box(
        modifier = modifier.imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AuthHeader(isRegistration = isRegistration)

            if (isRegistration) {
                AuthTextField(
                    value = state.name,
                    onValueChange = {
                        onFieldChange(AuthField.NAME, it)
                    },
                    label = "Ім’я",
                    error = state.errors[AuthField.NAME],
                    enabled = fieldsEnabled
                )
            }

            AuthTextField(
                value = state.email,
                onValueChange = {
                    onFieldChange(AuthField.EMAIL, it)
                },
                label = "Email",
                error = state.errors[AuthField.EMAIL],
                enabled = fieldsEnabled,
                keyboardType = KeyboardType.Email
            )

            AuthPasswordField(
                value = state.password,
                onValueChange = {
                    onFieldChange(AuthField.PASSWORD, it)
                },
                label = "Пароль",
                error = state.errors[AuthField.PASSWORD],
                enabled = fieldsEnabled
            )

            if (isRegistration) {
                Text(
                    text = "Пароль: 8–64 символи, " +
                            "хоча б одна літера та одна цифра.",
                    style = MaterialTheme.typography.bodySmall
                )

                AuthPasswordField(
                    value = state.confirmPassword,
                    onValueChange = {
                        onFieldChange(AuthField.CONFIRM_PASSWORD, it)
                    },
                    label = "Підтвердження пароля",
                    error = state.errors[AuthField.CONFIRM_PASSWORD],
                    enabled = fieldsEnabled
                )
            }

            AuthMessage(
                message = state.message,
                isError = state.messageIsError
            )

            Button(
                onClick = onSubmit,
                enabled = fieldsEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current
                    )

                    Spacer(Modifier.width(10.dp))

                    Text("Обробка…")
                } else {
                    Text(
                        if (isRegistration) {
                            "Зареєструватися"
                        } else {
                            "Увійти"
                        }
                    )
                }
            }

            TextButton(
                onClick = onSwitchMode,
                enabled = fieldsEnabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (isRegistration) {
                        "Уже є акаунт? Увійти"
                    } else {
                        "Немає акаунта? Зареєструватися"
                    }
                )
            }

            Text(
                text = "Навчальний режим. Акаунти зберігаються " +
                        "лише в пам’яті поточного сеансу.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!isRegistration) {
                Text(
                    text = "Демовхід:\n" +
                            "Email: ${DemoAuthRepository.DEMO_EMAIL}\n" +
                            "Пароль: ${DemoAuthRepository.DEMO_PASSWORD}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Показує назву застосунку та заголовок поточного режиму авторизації.
 */
@Composable
private fun AuthHeader(
    isRegistration: Boolean
) {
    Image(
        painter = painterResource(
            id = R.drawable.energy_illustration
        ),
        contentDescription = null,
        modifier = Modifier.size(64.dp)
    )

    Text(
        text = "EnergyForecast",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.primary
    )

    Text(
        text = if (isRegistration) {
            "Створення акаунта"
        } else {
            "Вхід до системи"
        },
        style = MaterialTheme.typography.titleLarge
    )
}

/**
 * Показує результат операції; тип повідомлення визначає стан, а не його текст.
 */
@Composable
private fun AuthMessage(
    message: String?,
    isError: Boolean
) {
    if (message != null) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            }
        )
    }
}