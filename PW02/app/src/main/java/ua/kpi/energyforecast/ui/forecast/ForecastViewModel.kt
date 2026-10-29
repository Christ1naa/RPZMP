package ua.kpi.energyforecast.ui.forecast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ua.kpi.energyforecast.logic.DeviceValidator
import ua.kpi.energyforecast.logic.EnergyCalculator
import ua.kpi.energyforecast.model.EnergyDevice

/**
 * Керує списком приладів і прогнозом у пам’яті.
 * Екран отримує стан через StateFlow та передає дії користувача.
 */
class ForecastViewModel(
    private val calculationDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(ForecastUiState())

    val uiState: StateFlow<ForecastUiState> = mutableUiState.asStateFlow()

    private var calculationJob: Job? = null

    /** Зберігає введення періоду та запускає перерахунок для валідного значення. */
    fun updateDays(value: String) {
        recalculate(
            mutableUiState.value.copy(daysText = value)
        )
    }

    /**
     * Перевіряє параметри перед зміною списку.
     * Наявний id означає редагування позиції, новий — додавання.
     */
    fun saveDevice(device: EnergyDevice) {
        val current = mutableUiState.value

        val validation = DeviceValidator.validate(
            nameText = device.name,
            powerText = device.powerWatts.toString(),
            hoursText = device.hoursPerDay.toString(),
            countText = device.deviceCount.toString()
        )

        if (device.id.isBlank() || validation.parameters == null) {
            mutableUiState.value = current.copy(
                error = "Не вдалося зберегти: перевірте параметри приладу."
            )
            return
        }

        val exists = current.devices.any {
            it.id == device.id
        }

        if (!exists && !current.canAddDevice) {
            mutableUiState.value = current.copy(
                error = "Досягнуто межі: 50 позицій."
            )
            return
        }

        val normalizedDevice = device.copy(
            name = validation.name
        )

        val updatedDevices = if (exists) {
            current.devices.map { existingDevice ->
                if (existingDevice.id == device.id) {
                    normalizedDevice
                } else {
                    existingDevice
                }
            }
        } else {
            current.devices + normalizedDevice
        }

        recalculate(
            current.copy(devices = updatedDevices)
        )
    }

    /** Видаляє прилад за id та оновлює результат. */
    fun deleteDevice(id: String) {
        val current = mutableUiState.value

        recalculate(
            current.copy(
                devices = current.devices.filterNot {
                    it.id == id
                }
            )
        )
    }

    /**
     * Працює зі знімком параметрів і скасовує застарілий розрахунок.
     * Порожній список або невалідний період приховують результат.
     */
    private fun recalculate(next: ForecastUiState) {
        calculationJob?.cancel()

        val days = next.forecastDays

        if (next.devices.isEmpty() || days == null) {
            mutableUiState.value = next.copy(
                result = null,
                isLoading = false,
                error = null
            )
            return
        }

        mutableUiState.value = next.copy(
            result = null,
            isLoading = true,
            error = null
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
                // Скасування застарілого розрахунку не показуємо як помилку.
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
}