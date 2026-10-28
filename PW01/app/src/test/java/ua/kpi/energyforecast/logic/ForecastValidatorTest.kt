package ua.kpi.energyforecast.logic

import org.junit.Assert.*
import org.junit.Test

class ForecastValidatorTest {

    @Test
    fun decimalCommaIsAccepted() {
        val result = ForecastValidator.validate(
            powerText = "1500",
            hoursText = "1,5",
            countText = "2",
            daysText = "10"
        )

        assertTrue(result.errors.isEmpty())

        val parameters = requireNotNull(result.parameters)

        assertEquals(
            1.5,
            parameters.hoursPerDay,
            0.000001
        )

        val forecast = EnergyCalculator.calculate(parameters)

        assertEquals(
            4.5,
            forecast.dailyConsumptionKwh,
            0.000001
        )

        assertEquals(
            45.0,
            forecast.periodConsumptionKwh,
            0.000001
        )
    }

    @Test
    fun invalidPowerValuesAreRejected() {
        val invalidValues = listOf(
            "",
            "abc",
            "0",
            "-10",
            "100001",
            "NaN",
            "Infinity"
        )

        invalidValues.forEach { value ->
            val result = ForecastValidator.validate(
                powerText = value,
                hoursText = "2",
                countText = "1",
                daysText = "30"
            )

            assertNull(result.parameters)

            assertTrue(
                "Expected invalid power: $value",
                result.errors.containsKey(ForecastValidator.POWER)
            )
        }
    }

    @Test
    fun invalidHoursQuantityAndPeriodAreRejected() {
        val result = ForecastValidator.validate(
            powerText = "1000",
            hoursText = "25",
            countText = "1.5",
            daysText = "366"
        )

        assertNull(result.parameters)

        assertTrue(
            result.errors.containsKey(ForecastValidator.HOURS)
        )

        assertTrue(
            result.errors.containsKey(ForecastValidator.COUNT)
        )

        assertTrue(
            result.errors.containsKey(ForecastValidator.DAYS)
        )
    }

    @Test
    fun allowedBoundaryValuesAreAccepted() {
        val result = ForecastValidator.validate(
            powerText = "100000",
            hoursText = "24",
            countText = "1000",
            daysText = "365"
        )

        assertTrue(result.errors.isEmpty())
        assertNotNull(result.parameters)
    }
}