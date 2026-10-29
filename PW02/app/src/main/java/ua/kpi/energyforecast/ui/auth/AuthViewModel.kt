package ua.kpi.energyforecast.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ua.kpi.energyforecast.data.AuthResult
import ua.kpi.energyforecast.data.DemoAuthRepository
import ua.kpi.energyforecast.logic.AuthValidator
import ua.kpi.energyforecast.model.AuthField
import ua.kpi.energyforecast.model.AuthMode
import ua.kpi.energyforecast.model.AuthUiState

/**
 * Власник стану auth-форми й акаунтів поточного сеансу; Compose реагує на зміни uiState.
 * Переживає поворот Activity, але не відновлює акаунти чи сесію після process death.
 */
class AuthViewModel : ViewModel() {

    private val repository = DemoAuthRepository()

    var uiState by mutableStateOf(AuthUiState())
        private set

    /**
     * Після невдалої валідації повторно перевіряє форму, зберігаючи помилки інших полів.
     * Під час loading ігнорує зміни, щоб результат відповідав надісланому введенню.
     */
    fun updateField(
        field: AuthField,
        value: String
    ) {
        if (uiState.isLoading) return

        val updated = when (field) {
            AuthField.NAME -> uiState.copy(name = value)

            AuthField.EMAIL -> uiState.copy(email = value)

            AuthField.PASSWORD -> uiState.copy(password = value)

            AuthField.CONFIRM_PASSWORD -> {
                uiState.copy(confirmPassword = value)
            }
        }

        val updatedErrors = if (uiState.errors.isNotEmpty()) {
            when (updated.mode) {
                AuthMode.LOGIN -> {
                    AuthValidator.validateLogin(
                        email = updated.email,
                        password = updated.password
                    )
                }

                AuthMode.REGISTER -> {
                    AuthValidator.validateRegistration(
                        name = updated.name,
                        email = updated.email,
                        password = updated.password,
                        confirmPassword = updated.confirmPassword
                    )
                }
            }
        } else {
            emptyMap()
        }

        uiState = updated.copy(
            errors = updatedErrors,
            message = null,
            messageIsError = false
        )
    }

    /** Перемикає форму, зберігаючи email, але очищуючи паролі та попередні повідомлення. */
    fun switchMode() {
        if (uiState.isLoading) return

        uiState = AuthUiState(
            mode = if (uiState.mode == AuthMode.LOGIN) {
                AuthMode.REGISTER
            } else {
                AuthMode.LOGIN
            },
            email = uiState.email
        )
    }

    /**
     * Перевіряє snapshot форми; невалідні дані не запускають обробку.
     * Реєстрація повертає до входу, успішний вхід відкриває основний UI через user.
     */
    fun submit() {
        if (uiState.isLoading) return

        val submittedState = uiState

        val errors = when (submittedState.mode) {
            AuthMode.LOGIN -> {
                AuthValidator.validateLogin(
                    email = submittedState.email,
                    password = submittedState.password
                )
            }

            AuthMode.REGISTER -> {
                AuthValidator.validateRegistration(
                    name = submittedState.name,
                    email = submittedState.email,
                    password = submittedState.password,
                    confirmPassword = submittedState.confirmPassword
                )
            }
        }

        uiState = uiState.copy(
            errors = errors,
            message = null,
            messageIsError = false
        )

        if (errors.isNotEmpty()) return

        // Синхронне блокування до launch захищає від повторного submit навіть до старту coroutine.
        uiState = uiState.copy(isLoading = true)

        viewModelScope.launch {
            try {
                // Навчальна затримка показує loading, не блокуючи головний потік.
                delay(1200)

                val result = when (submittedState.mode) {
                    AuthMode.LOGIN -> repository.login(
                        email = submittedState.email,
                        password = submittedState.password
                    )

                    AuthMode.REGISTER -> repository.register(
                        name = submittedState.name,
                        email = submittedState.email,
                        password = submittedState.password
                    )
                }

                when (result) {
                    is AuthResult.Success -> {
                        uiState = if (
                            submittedState.mode == AuthMode.REGISTER
                        ) {
                            AuthUiState(
                                mode = AuthMode.LOGIN,
                                email = result.user.email,
                                message = "Реєстрація успішна. " +
                                        "Увійдіть зі своїм паролем."
                            )
                        } else {
                            AuthUiState(user = result.user)
                        }
                    }

                    is AuthResult.Failure -> {
                        uiState = uiState.copy(
                            isLoading = false,
                            message = result.message,
                            messageIsError = true
                        )
                    }
                }
            } catch (exception: CancellationException) {
                // Скасування ViewModel не перетворюємо на помилку авторизації.
                throw exception
            } catch (exception: Exception) {
                uiState = uiState.copy(
                    isLoading = false,
                    message = "Не вдалося виконати операцію. " +
                            "Спробуйте ще раз.",
                    messageIsError = true
                )
            }
        }
    }

    /** Очищує auth-стан; акаунти залишаються в repository до знищення ViewModel. */
    fun logout() {
        uiState = AuthUiState()
    }
}
