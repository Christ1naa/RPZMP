package ua.kpi.energyforecast.ui.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.kpi.energyforecast.data.EnergyRepository
import ua.kpi.energyforecast.logic.DeviceValidator
import ua.kpi.energyforecast.model.EnergyDevice

/**
 * Отримує збережений список через Repository.
 * Введення періоду залишається локальним, а валідні значення записуються у сховище.
 */
class ForecastViewModel(
    private val repository: EnergyRepository,
    private val ownerKey: String,
    private val calculationDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(
        ForecastUiState(isLoading = true)
    )

    val uiState: StateFlow<ForecastUiState> = mutableUiState.asStateFlow()

    private var calculationJob: Job? = null

    private var observationJob: Job? = null
    private var profileReceived = false
    private var daysEdited = false

    /**
     * Записи виконуються в порядку дій користувача.
     * Нове введення не скасовує вже розпочатий запис у базу.
     */
    private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        require(ownerKey.isNotBlank())

        viewModelScope.launch {
            for (write in writes) {
                try {
                    write()
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    mutableUiState.value = mutableUiState.value.copy(
                        error = "Не вдалося зберегти зміни. Спробуйте ще раз."
                    )
                }
            }
        }

        observeProfile()
    }

    /** Створює нову підписку після помилки читання зі сховища. */
    fun retryLoad() {
        if (mutableUiState.value.loadError == null) return
        observeProfile()
    }

    /**
     * Одночасно працює лише одна підписка на профіль.
     * Повторне завантаження зберігає введення користувача та параметри списку.
     */
    private fun observeProfile() {
        observationJob?.cancel()
        calculationJob?.cancel()

        mutableUiState.value = mutableUiState.value.copy(
            isLoading = true,
            loadError = null
        )

        observationJob = viewModelScope.launch {
            try {
                repository.observeProfile(ownerKey).collect { saved ->
                    val current = mutableUiState.value

                    // Перша відповідь не повинна стирати вже введений період.
                    val daysText = if (!profileReceived && !daysEdited) {
                        saved.forecastDays.toString()
                    } else {
                        current.daysText
                    }

                    profileReceived = true

                    recalculate(
                        current.copy(
                            devices = saved.devices,
                            daysText = daysText,
                            loadError = null
                        )
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                calculationJob?.cancel()

                mutableUiState.value = mutableUiState.value.copy(
                    isLoading = false,
                    loadError = "Не вдалося завантажити збережені дані."
                )
            }
        }
    }

    /** Звичайне завантаження використовує кеш, оновлення перечитує джерело. */
    fun loadDemoScenarios(refresh: Boolean = false) {
        val current = mutableUiState.value

        if (current.isLoadingDemos || current.isImportingDemo) return

        mutableUiState.value = current.copy(
            isLoadingDemos = true,
            demoError = null,
            demoMessage = null
        )

        viewModelScope.launch {
            try {
                val scenarios = repository.loadDemoScenarios(refresh)

                mutableUiState.value = mutableUiState.value.copy(
                    demoScenarios = scenarios,
                    isLoadingDemos = false
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableUiState.value = mutableUiState.value.copy(
                    isLoadingDemos = false,
                    demoError = "Не вдалося завантажити приклади. Спробуйте ще раз."
                )
            }
        }
    }

    /**
     * Імпорт виконується в черзі записів.
     * Повторне натискання блокується до завершення операції.
     */
    fun importDemoScenario(scenarioId: String) {
        val current = mutableUiState.value

        if (
            current.isImportingDemo ||
            current.isLoadingDemos ||
            current.isLoading ||
            current.loadError != null
        ) return

        val scenario = current.demoScenarios.firstOrNull {
            it.id == scenarioId
        }

        if (scenario == null) {
            mutableUiState.value = current.copy(
                demoError = "Приклад не знайдено. Оновіть список."
            )
            return
        }

        mutableUiState.value = current.copy(
            isImportingDemo = true,
            demoError = null,
            demoMessage = null
        )

        enqueueWrite {
            try {
                repository.importDemoScenario(ownerKey, scenarioId)

                mutableUiState.value = mutableUiState.value.copy(
                    searchQuery = "",
                    demoMessage = "Прилади з прикладу «${scenario.name}» додано."
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: IllegalArgumentException) {
                mutableUiState.value = mutableUiState.value.copy(
                    demoError = exception.message
                        ?: "Не вдалося імпортувати приклад."
                )
            } catch (exception: Exception) {
                mutableUiState.value = mutableUiState.value.copy(
                    demoError = "Не вдалося імпортувати приклад. Спробуйте ще раз."
                )
            } finally {
                mutableUiState.value = mutableUiState.value.copy(
                    isImportingDemo = false
                )
            }
        }
    }

    /** Змінює відображення списку без запису в Room і перерахунку прогнозу. */
    fun updateSearchQuery(value: String) {
        mutableUiState.value = mutableUiState.value.copy(
            searchQuery = value
        )
    }

    fun updateSort(value: DeviceSort) {
        mutableUiState.value = mutableUiState.value.copy(
            sort = value
        )
    }

    /** Невалідне введення показується у формі, але не записується у базу. */
    fun updateDays(value: String) {
        daysEdited = true

        recalculate(
            mutableUiState.value.copy(daysText = value)
        )

        val days = DeviceValidator.parseForecastDays(value) ?: return

        enqueueWrite {
            repository.saveForecastDays(ownerKey, days)
        }
    }

    /** Список оновиться після підтвердженого запису через Flow зі сховища. */
    fun saveDevice(device: EnergyDevice) {
        val validation = DeviceValidator.validate(
            nameText = device.name,
            powerText = device.powerWatts.toString(),
            hoursText = device.hoursPerDay.toString(),
            countText = device.deviceCount.toString()
        )

        if (device.id.isBlank() || validation.parameters == null) {
            mutableUiState.value = mutableUiState.value.copy(
                error = "Не вдалося зберегти: перевірте параметри приладу."
            )
            return
        }

        val normalizedDevice = device.copy(name = validation.name)

        enqueueWrite {
            repository.saveDevice(ownerKey, normalizedDevice)
        }
    }

    fun deleteDevice(id: String) {
        enqueueWrite {
            repository.deleteDevice(ownerKey, id)
        }
    }

    private fun enqueueWrite(write: suspend () -> Unit) {
        mutableUiState.value = mutableUiState.value.copy(error = null)
        writes.trySend(write)
    }

    /** Скасовує застаріле обчислення, зберігаючи актуальне введення. */
    private fun recalculate(next: ForecastUiState) {
        calculationJob?.cancel()

        val days = next.forecastDays

        if (next.devices.isEmpty() || days == null) {
            mutableUiState.value = next.copy(
                result = null,
                isLoading = false
            )
            return
        }

        mutableUiState.value = next.copy(
            result = null,
            isLoading = true
        )

        calculationJob = viewModelScope.launch {
            try {
                val result = withContext(calculationDispatcher) {
                    repository.calculateForecast(
                        devices = next.devices,
                        forecastDays = days
                    )
                }

                mutableUiState.value = mutableUiState.value.copy(
                    result = result,
                    isLoading = false
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableUiState.value = mutableUiState.value.copy(
                    result = null,
                    isLoading = false,
                    error = "Не вдалося обчислити прогноз."
                )
            }
        }
    }

    override fun onCleared() {
        writes.close()
        super.onCleared()
    }
}