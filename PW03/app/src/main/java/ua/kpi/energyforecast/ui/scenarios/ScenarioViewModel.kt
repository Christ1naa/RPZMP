package ua.kpi.energyforecast.ui.scenarios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import ua.kpi.energyforecast.data.ScenarioImportRepository
import ua.kpi.energyforecast.data.ScenarioRepository
import ua.kpi.energyforecast.model.ScenarioCategory

class ScenarioViewModel(
    private val repository: ScenarioRepository,
    private val importer: ScenarioImportRepository,
    private val ownerKey: String
) : ViewModel() {

    private val mutableState = MutableStateFlow(ScenarioUiState())
    val uiState = mutableState.asStateFlow()

    private var observationJob: Job? = null

    init {
        require(ownerKey.isNotBlank())
        retryCache()
    }

    /** Читання Room не залежить від успішності мережевого оновлення. */
    fun retryCache() {
        observationJob?.cancel()

        mutableState.update {
            it.copy(isLoadingCache = true, cacheError = null)
        }

        observationJob = viewModelScope.launch {
            try {
                repository.observeCatalog().collect { catalog ->
                    mutableState.update {
                        it.copy(
                            catalog = catalog,
                            isLoadingCache = false,
                            cacheError = null
                        )
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableState.update {
                    it.copy(
                        isLoadingCache = false,
                        cacheError = "Не вдалося прочитати збережений каталог."
                    )
                }
            }
        }
    }

    fun refresh() {
        if (mutableState.value.isRefreshing) return

        mutableState.update {
            it.copy(
                isRefreshing = true,
                refreshError = null,
                message = null
            )
        }

        viewModelScope.launch {
            try {
                repository.refresh()

                mutableState.update {
                    it.copy(
                        isRefreshing = false,
                        message = "Каталог оновлено."
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                val message = when {
                    exception is HttpException && exception.code() == 401 ->
                        "Сервер не підтвердив авторизацію."

                    else ->
                        "Не вдалося оновити каталог. " +
                                "Збережені дані залишаються доступними."
                }

                mutableState.update {
                    it.copy(
                        isRefreshing = false,
                        refreshError = message
                    )
                }
            }
        }
    }

    fun updateQuery(value: String) {
        mutableState.update { it.copy(query = value) }
    }

    fun updateCategory(value: ScenarioCategory?) {
        mutableState.update { it.copy(category = value) }
    }

    fun updateSort(value: ScenarioSort) {
        mutableState.update { it.copy(sort = value) }
    }

    fun selectScenario(id: String) {
        mutableState.update {
            it.copy(selectedScenarioId = id, message = null)
        }
    }

    fun updateDays(value: String) {
        mutableState.update { it.copy(daysText = value) }
    }

    fun importScenario(id: String) {
        if (mutableState.value.isImporting) return

        mutableState.update {
            it.copy(
                importingScenarioId = id,
                message = null
            )
        }

        viewModelScope.launch {
            try {
                importer.importScenario(ownerKey, id)

                mutableState.update {
                    it.copy(
                        importingScenarioId = null,
                        message = "Прилади додано до вашого профілю."
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: IllegalArgumentException) {
                mutableState.update {
                    it.copy(
                        importingScenarioId = null,
                        message = exception.message
                            ?: "Не вдалося імпортувати сценарій."
                    )
                }
            } catch (exception: Exception) {
                mutableState.update {
                    it.copy(
                        importingScenarioId = null,
                        message = "Не вдалося зберегти прилади. Спробуйте ще раз."
                    )
                }
            }
        }
    }
    fun networkPermissionDenied() {
        mutableState.update {
            it.copy(
                refreshError = "Для оновлення каталогу потрібен " +
                        "дозвіл локальної мережі."
            )
        }
    }
}