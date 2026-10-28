package ua.kpi.energyforecast.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import ua.kpi.energyforecast.model.EnergyDevice
import ua.kpi.energyforecast.model.ForecastParameters

class EnergyCalculatorTest {

    @Test
    fun nonfinitePowerIsRejected() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)
            .forEach { power ->
                assertThrows(IllegalArgumentException::class.java) {
                    EnergyCalculator.calculate(ForecastParameters(power, 2.0, 1, 30))
                }
            }
    }

    @Test
    fun nonfiniteAndOutOfRangeHoursAreRejected() {
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -0.1, 24.1)
            .forEach { hours ->
                assertThrows(IllegalArgumentException::class.java) {
                    EnergyCalculator.calculate(ForecastParameters(1000.0, hours, 1, 30))
                }
            }
    }

    @Test
    fun finiteInputsWhoseResultOverflowsAreRejected() {
        // Перший набір переповнює добову енергію, другий — лише енергію за період.
        listOf(
            ForecastParameters(Double.MAX_VALUE, 24.0, 1000, 1),
            ForecastParameters(Double.MAX_VALUE, 24.0, 1, 365)
        ).forEach { parameters ->
            assertThrows(IllegalArgumentException::class.java) {
                EnergyCalculator.calculate(parameters)
            }
        }
    }

    @Test
    fun totalRejectsPeriodsOutsideAllowedRangeEvenForEmptyList() {
        listOf(0, 366).forEach { days ->
            assertThrows(IllegalArgumentException::class.java) {
                EnergyCalculator.calculateTotal(emptyList(), days)
            }
        }
    }

    @Test
    fun emptyListProducesZeroAtBothPeriodBoundaries() {
        listOf(1, 365).forEach { days ->
            val result = EnergyCalculator.calculateTotal(emptyList(), days)

            assertEquals(0.0, result.dailyConsumptionKwh, 0.0)
            assertEquals(0.0, result.periodConsumptionKwh, 0.0)
            assertEquals(days, result.forecastDays)
        }
    }

    @Test
    fun maximumUiInputsRemainFiniteAndIncludeAllQuantities() {
        val devices = List(50) { index ->
            EnergyDevice("device-$index", "Прилад", 100000.0, 24.0, 1000)
        }

        val result = EnergyCalculator.calculateTotal(devices, 365)

        assertEquals(120000000.0, result.dailyConsumptionKwh, 0.000001)
        assertEquals(43800000000.0, result.periodConsumptionKwh, 0.000001)
    }

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
