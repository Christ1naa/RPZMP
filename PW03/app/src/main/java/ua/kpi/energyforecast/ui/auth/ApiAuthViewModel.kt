package ua.kpi.energyforecast.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import ua.kpi.energyforecast.data.auth.ApiSessionRepository
import ua.kpi.energyforecast.data.auth.ApiSessionState
import ua.kpi.energyforecast.logic.AuthValidator
import ua.kpi.energyforecast.model.AuthField
import ua.kpi.energyforecast.data.remote.NetworkErrors
import ua.kpi.energyforecast.ui.UiMessages

data class ApiLoginUiState(
    val email: String = "",
    val password: String = "",
    val errors: Map<AuthField, String> = emptyMap(),
    val isSubmitting: Boolean = false,
    val error: String? = null
)

class ApiAuthViewModel(
    application: Application
) : AndroidViewModel(application) {

    val repository = ApiSessionRepository.getInstance(application)

    private val mutableForm = MutableStateFlow(ApiLoginUiState())
    val formState = mutableForm.asStateFlow()

    private val mutableSession = MutableStateFlow(ApiSessionState())
    val sessionState = mutableSession.asStateFlow()

    /**
     * Окрема область ViewModel для поточної сесії.
     * Переживає поворот Activity, але очищається після зміни сесії.
     */
    private val contentStore = ViewModelStore()

    val sessionContentOwner: ViewModelStoreOwner =
        object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore
                get() = contentStore
        }

    private var activeSessionKey: String? = null

    init {
        viewModelScope.launch {
            repository.state.collect { state ->
                val nextKey = state.session?.sessionKey

                if (nextKey != activeSessionKey) {
                    contentStore.clear()
                    activeSessionKey = nextKey
                }

                // UI отримує нову сесію після очищення старих ViewModel.
                mutableSession.value = state
            }
        }
    }

    fun updateEmail(value: String) {
        if (mutableForm.value.isSubmitting) return

        mutableForm.update {
            it.copy(email = value, errors = emptyMap(), error = null)
        }
    }

    fun updatePassword(value: String) {
        if (mutableForm.value.isSubmitting) return

        mutableForm.update {
            it.copy(password = value, errors = emptyMap(), error = null)
        }
    }

    fun permissionDenied() {
        mutableForm.update {
            it.copy(
                error = "Для підключення до навчального сервера " +
                        "потрібен дозвіл локальної мережі."
            )
        }
    }

    fun submit() {
        val submitted = mutableForm.value

        if (
            submitted.isSubmitting ||
            !mutableSession.value.isReady
        ) return

        val errors = AuthValidator.validateLogin(
            email = submitted.email,
            password = submitted.password
        )

        mutableForm.update {
            it.copy(errors = errors, error = null)
        }

        if (errors.isNotEmpty()) return

        mutableForm.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            try {
                repository.login(
                    email = submitted.email,
                    password = submitted.password
                )

                // Пароль після успішного входу більше не потрібний.
                mutableForm.value = ApiLoginUiState(
                    email = submitted.email
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                val message = NetworkErrors.message(
                    error = exception,
                    isLogin = true
                )

                UiMessages.show(message)

                mutableForm.update {
                    it.copy(
                        isSubmitting = false,
                        error = message
                    )
                }
            }
        }
    }

    fun logout() {
        if (mutableForm.value.isSubmitting) return

        mutableForm.update {
            it.copy(isSubmitting = true, error = null)
        }

        viewModelScope.launch {
            try {
                repository.logout()
                mutableForm.value = ApiLoginUiState()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableForm.update {
                    it.copy(
                        isSubmitting = false,
                        error = "Не вдалося зберегти вихід. Спробуйте ще раз."
                    )
                }
            }
        }
    }

    fun checkExpiry() {
        repository.checkExpiry()
    }

    override fun onCleared() {
        contentStore.clear()
        super.onCleared()
    }
}