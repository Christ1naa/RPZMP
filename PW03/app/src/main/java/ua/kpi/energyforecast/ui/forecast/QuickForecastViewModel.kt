package ua.kpi.energyforecast.ui.forecast

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import ua.kpi.energyforecast.logic.EnergyCalculator
import ua.kpi.energyforecast.logic.ForecastValidator
import ua.kpi.energyforecast.model.ForecastResult

/** Стан форми швидкого прогнозу та останній підтверджений результат. */
data class QuickForecastUiState(
    val powerText: String = "1000",
    val hoursText: String = "2",
    val countText: String = "1",
    val daysText: String = "30",
    val errors: Map<String, String> = emptyMap(),
    val result: ForecastResult? = null
)

/**
 * Керує введенням, валідацією та розрахунком швидкого прогнозу.
 * SavedStateHandle зберігає чернетку і факт підтвердження форми.
 */
class QuickForecastViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var validationRequested: Boolean =
        savedStateHandle["validationRequested"] ?: false

    private val mutableUiState = MutableStateFlow(
        QuickForecastUiState(
            powerText = savedStateHandle["powerText"] ?: "1000",
            hoursText = savedStateHandle["hoursText"] ?: "2",
            countText = savedStateHandle["countText"] ?: "1",
            daysText = savedStateHandle["daysText"]
                ?: (savedStateHandle.get<Int>("days") ?: 30).toString()
        )
    )

    val uiState = mutableUiState.asStateFlow()

    init {
        val state = mutableUiState.value
        val validation = validate(state)

        val result = if (
            savedStateHandle.get<Boolean>("hasCalculated") == true
        ) {
            validation.parameters?.let(EnergyCalculator::calculate)
        } else {
            null
        }

        mutableUiState.value = state.copy(
            errors = if (validationRequested) {
                validation.errors
            } else {
                emptyMap()
            },
            result = result
        )
    }

    /** Зміна параметра приховує результат, отриманий для попереднього введення. */
    fun updateField(field: String, value: String) {
        val current = mutableUiState.value

        val updated = when (field) {
            ForecastValidator.POWER -> {
                savedStateHandle["powerText"] = value
                current.copy(powerText = value)
            }

            ForecastValidator.HOURS -> {
                savedStateHandle["hoursText"] = value
                current.copy(hoursText = value)
            }

            ForecastValidator.COUNT -> {
                savedStateHandle["countText"] = value
                current.copy(countText = value)
            }

            ForecastValidator.DAYS -> {
                savedStateHandle["daysText"] = value
                current.copy(daysText = value)
            }

            else -> error("Невідоме поле: $field")
        }

        savedStateHandle["hasCalculated"] = false

        mutableUiState.value = updated.copy(
            errors = if (validationRequested) {
                validate(updated).errors
            } else {
                emptyMap()
            },
            result = null
        )
    }

    /**
     * Виконує розрахунок лише після підтвердження валідної форми.
     * Обчислення коротке, тому окрема корутина для нього не потрібна.
     */
    fun calculate(): ForecastResult? {
        validationRequested = true
        savedStateHandle["validationRequested"] = true

        val current = mutableUiState.value
        val validation = validate(current)

        val result = validation.parameters?.let(
            EnergyCalculator::calculate
        )

        savedStateHandle["hasCalculated"] = result != null

        mutableUiState.value = current.copy(
            errors = validation.errors,
            result = result
        )

        return result
    }

    private fun validate(state: QuickForecastUiState) =
        ForecastValidator.validate(
            powerText = state.powerText,
            hoursText = state.hoursText,
            countText = state.countText,
            daysText = state.daysText
        )
}