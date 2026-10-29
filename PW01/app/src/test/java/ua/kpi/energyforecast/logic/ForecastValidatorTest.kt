package ua.kpi.energyforecast.logic

import org.junit.Assert.*
import org.junit.Test

class ForecastValidatorTest {

    @Test
    fun nonfiniteAndOutOfRangeHoursAreRejected() {
        listOf("NaN", "Infinity", "-Infinity", "-0.1", "24.1").forEach { hours ->
            val result = ForecastValidator.validate("1000", hours, "1", "30")

            assertNull(result.parameters)
            assertEquals(setOf(ForecastValidator.HOURS), result.errors.keys)
        }
    }

    @Test
    fun quantitiesAndPeriodsRejectOutOfRangeAndOverflowingIntegers() {
        // Незалежна перевірка полів не дозволяє одній помилці приховати іншу.
        listOf("-1", "0", "1001", "2147483648").forEach { count ->
            val result = ForecastValidator.validate("1000", "2", count, "30")

            assertNull(result.parameters)
            assertEquals(setOf(ForecastValidator.COUNT), result.errors.keys)
        }

        listOf("-1", "0", "366", "2147483648").forEach { days ->
            val result = ForecastValidator.validate("1000", "2", "1", days)

            assertNull(result.parameters)
            assertEquals(setOf(ForecastValidator.DAYS), result.errors.keys)
        }
    }

    @Test
    fun zeroHoursAndMinimumQuantityAndPeriodAreAccepted() {
        val result = ForecastValidator.validate("0.1", "0", "1", "1")

        assertTrue(result.errors.isEmpty())
        val parameters = requireNotNull(result.parameters)
        assertEquals(0.1, parameters.powerWatts, 0.000001)
        assertEquals(0.0, parameters.hoursPerDay, 0.0)
        assertEquals(1, parameters.deviceCount)
        assertEquals(1, parameters.forecastDays)
    }

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
