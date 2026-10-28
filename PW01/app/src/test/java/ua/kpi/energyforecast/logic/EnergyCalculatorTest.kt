package ua.kpi.energyforecast.logic

import org.junit.Assert.assertEquals
import org.junit.Test
import ua.kpi.energyforecast.model.EnergyDevice
import ua.kpi.energyforecast.model.ForecastParameters

class EnergyCalculatorTest {

    @Test
    fun singleDeviceCalculatesDailyAndPeriodConsumption() {
        val result = EnergyCalculator.calculate(
            ForecastParameters(
                powerWatts = 1000.0,
                hoursPerDay = 2.0,
                deviceCount = 1,
                forecastDays = 30
            )
        )

        assertEquals(2.0, result.dailyConsumptionKwh, 0.000001)
        assertEquals(60.0, result.periodConsumptionKwh, 0.000001)
        assertEquals(30, result.forecastDays)
    }

    @Test
    fun multipleDevicesAreSummedCorrectly() {
        val devices = listOf(
            EnergyDevice(
                id = "heater",
                name = "Обігрівач",
                powerWatts = 1000.0,
                hoursPerDay = 2.0,
                deviceCount = 1
            ),
            EnergyDevice(
                id = "lamps",
                name = "Лампа",
                powerWatts = 100.0,
                hoursPerDay = 5.0,
                deviceCount = 3
            )
        )

        val result = EnergyCalculator.calculateTotal(
            devices = devices,
            forecastDays = 30
        )

        assertEquals(3.5, result.dailyConsumptionKwh, 0.000001)
        assertEquals(105.0, result.periodConsumptionKwh, 0.000001)
    }

    @Test
    fun zeroWorkingHoursProduceZeroConsumption() {
        val result = EnergyCalculator.calculate(
            ForecastParameters(
                powerWatts = 1500.0,
                hoursPerDay = 0.0,
                deviceCount = 2,
                forecastDays = 30
            )
        )

        assertEquals(0.0, result.dailyConsumptionKwh, 0.000001)
        assertEquals(0.0, result.periodConsumptionKwh, 0.000001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativePowerIsRejected() {
        EnergyCalculator.calculate(
            ForecastParameters(
                powerWatts = -100.0,
                hoursPerDay = 2.0,
                deviceCount = 1,
                forecastDays = 30
            )
        )
    }
}