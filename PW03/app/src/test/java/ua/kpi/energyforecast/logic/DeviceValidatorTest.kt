package ua.kpi.energyforecast.logic

import org.junit.Assert.*
import org.junit.Test

class DeviceValidatorTest {

    @Test
    fun emptyDeviceNameIsRejected() {
        val result = DeviceValidator.validate(
            nameText = "   ",
            powerText = "100",
            hoursText = "5",
            countText = "3"
        )

        assertNull(result.parameters)
        assertTrue(result.errors.containsKey(DeviceValidator.NAME))
    }

    @Test
    fun validDeviceNameIsTrimmed() {
        val result = DeviceValidator.validate(
            nameText = "  Лампа  ",
            powerText = "100",
            hoursText = "5",
            countText = "3"
        )

        assertEquals("Лампа", result.name)
        assertTrue(result.errors.isEmpty())
        assertNotNull(result.parameters)
    }
}