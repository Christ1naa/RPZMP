package ua.kpi.energyforecast.ui.forecast

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.*
import org.junit.Test
import ua.kpi.energyforecast.logic.ForecastValidator

class QuickForecastViewModelTest {

    @Test
    fun navigationPeriodIsUsedForCalculation() {
        val viewModel = QuickForecastViewModel(
            SavedStateHandle(mapOf("days" to 7))
        )

        assertEquals("7", viewModel.uiState.value.daysText)
        assertNull(viewModel.uiState.value.result)

        val result = requireNotNull(viewModel.calculate())

        assertEquals(7, result.forecastDays)
        assertEquals(14.0, result.periodConsumptionKwh, 0.000001)
        assertEquals(result, viewModel.uiState.value.result)
    }

    @Test
    fun invalidInputShowsErrorAndCanBeCorrected() {
        val viewModel = QuickForecastViewModel(SavedStateHandle())

        viewModel.updateField(ForecastValidator.POWER, "0")

        // До першого підтвердження помилки приховані.
        assertTrue(viewModel.uiState.value.errors.isEmpty())
        assertNull(viewModel.calculate())

        assertTrue(
            viewModel.uiState.value.errors.containsKey(
                ForecastValidator.POWER
            )
        )

        viewModel.updateField(ForecastValidator.POWER, "1500")

        assertTrue(viewModel.uiState.value.errors.isEmpty())
        assertNull(viewModel.uiState.value.result)

        assertEquals(
            90.0,
            requireNotNull(viewModel.calculate()).periodConsumptionKwh,
            0.000001
        )
    }

    @Test
    fun editingInputClearsPreviousResult() {
        val viewModel = QuickForecastViewModel(SavedStateHandle())

        viewModel.calculate()
        assertNotNull(viewModel.uiState.value.result)

        viewModel.updateField(ForecastValidator.HOURS, "3")

        assertNull(viewModel.uiState.value.result)

        assertEquals(
            90.0,
            requireNotNull(viewModel.calculate()).periodConsumptionKwh,
            0.000001
        )
    }

    @Test
    fun restoredStateKeepsConfirmedResultAndEditedPeriod() {
        val handle = SavedStateHandle(
            mapOf(
                "days" to 7,
                "powerText" to "1500",
                "hoursText" to "1,5",
                "countText" to "2",
                "daysText" to "10",
                "validationRequested" to true,
                "hasCalculated" to true
            )
        )

        val viewModel = QuickForecastViewModel(handle)
        val state = viewModel.uiState.value

        assertEquals("10", state.daysText)
        assertTrue(state.errors.isEmpty())
        assertEquals(
            45.0,
            requireNotNull(state.result).periodConsumptionKwh,
            0.000001
        )
    }
}