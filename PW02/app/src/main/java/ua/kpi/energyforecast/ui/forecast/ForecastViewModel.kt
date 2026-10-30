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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.kpi.energyforecast.data.EnergyRepository
import ua.kpi.energyforecast.logic.DeviceValidator
import ua.kpi.energyforecast.logic.EnergyCalculator
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

        viewModelScope.launch {
            repository.observeProfile(ownerKey)
                .catch { exception ->
                    if (exception is CancellationException) {
                        throw exception
                    }

                    calculationJob?.cancel()
                    mutableUiState.value = mutableUiState.value.copy(
                        isLoading = false,
                        error = "Не вдалося завантажити збережені дані."
                    )
                }
                .collect { saved ->
                    val current = mutableUiState.value

                    // Пізня відповідь бази не повинна стирати введення користувача.
                    val daysText = if (!profileReceived && !daysEdited) {
                        saved.forecastDays.toString()
                    } else {
                        current.daysText
                    }

                    profileReceived = true

                    recalculate(
                        current.copy(
                            devices = saved.devices,
                            daysText = daysText
                        )
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
                    EnergyCalculator.calculateTotal(
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