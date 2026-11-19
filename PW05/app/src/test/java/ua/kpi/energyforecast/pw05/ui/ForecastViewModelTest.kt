package ua.kpi.energyforecast.pw05.ui

import org.junit.Assert.*
import org.junit.Test

class ForecastViewModelTest {
    @Test fun changingHorizonUpdatesObservableForecast() {
        val model = ForecastViewModel()
        val daily = model.state.value
        assertEquals(24, daily.hours)
        model.setHorizon(168)
        assertEquals(168, model.state.value.hours)
        assertEquals(daily.forecastKwh * 7, model.state.value.forecastKwh, 0.00001)
        model.setHorizon(24)
        assertEquals(daily, model.state.value)
    }
    @Test fun invalidHorizonPreservesPreviousState() {
        val model = ForecastViewModel()
        val before = model.state.value
        assertThrows(IllegalArgumentException::class.java) { model.setHorizon(0) }
        assertEquals(before, model.state.value)
    }

    @Test fun validHorizonPersistsAndCanBeRestoredWithoutPersistingInvalidInput() {
        var stored = 168
        val model = ForecastViewModel(stored) { stored = it }
        assertEquals(168, model.state.value.hours)
        model.setHorizon(24)
        assertEquals(24, stored)
        assertThrows(IllegalArgumentException::class.java) { model.setHorizon(0) }
        assertEquals(24, stored)
        assertEquals(model.state.value, ForecastViewModel(stored).state.value)
    }
}
