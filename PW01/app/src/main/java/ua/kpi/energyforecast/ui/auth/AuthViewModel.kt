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

class AuthViewModel : ViewModel() {

    private val repository = DemoAuthRepository()

    var uiState by mutableStateOf(AuthUiState())
        private set

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

        uiState = updated.copy(
            errors = emptyMap(),
            message = null,
            messageIsError = false
        )
    }

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

        uiState = uiState.copy(isLoading = true)

        viewModelScope.launch {
            try {
                // Імітація запиту: UI залишається доступним для відображення.
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

    fun logout() {
        uiState = AuthUiState()
    }
}