package ua.kpi.energyforecast.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import ua.kpi.energyforecast.data.auth.UserRole
import ua.kpi.energyforecast.data.remote.AdminSummary
import ua.kpi.energyforecast.data.remote.EnergyApi
import ua.kpi.energyforecast.data.remote.InvalidApiDataException
import ua.kpi.energyforecast.data.remote.toSummary
import ua.kpi.energyforecast.data.remote.NetworkErrors
import ua.kpi.energyforecast.ui.UiMessages

data class AdminUiState(
    val isLoading: Boolean = false,
    val summary: AdminSummary? = null,
    val error: String? = null
)

/**
 * Завантажує статистику для адміністративного екрана.
 * Остаточне рішення про доступ приймає сервер.
 */
class AdminViewModel(
    private val api: EnergyApi,
    private val role: UserRole
) : ViewModel() {

    private val mutableState = MutableStateFlow(AdminUiState())
    val uiState = mutableState.asStateFlow()

    fun refresh() {
        if (mutableState.value.isLoading) return

        // Локальна перевірка не замінює серверну авторизацію.
        if (role != UserRole.ADMIN) {
            mutableState.value = AdminUiState(
                error = "Цей розділ доступний лише адміністратору."
            )
            return
        }

        mutableState.update {
            it.copy(isLoading = true, error = null)
        }

        viewModelScope.launch {
            try {
                val summary = api.getAdminSummary().toSummary()

                mutableState.value = AdminUiState(
                    summary = summary
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                val message = NetworkErrors.message(exception)

                UiMessages.show(message)

                val code = (exception as? HttpException)?.code()

                if (code == 401 || code == 403) {
                    mutableState.value = AdminUiState(
                        error = message
                    )
                } else {
                    showError(message)
                }
            }
        }
    }

    fun networkPermissionDenied() {
        showError(
            "Для з’єднання із сервером потрібен дозвіл на локальну мережу."
        )
    }

    private fun showError(message: String) {
        mutableState.update {
            it.copy(
                isLoading = false,
                error = message
            )
        }
    }
}