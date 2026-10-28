package ua.kpi.energyforecast.logic

import org.junit.Assert.*
import org.junit.Test
import ua.kpi.energyforecast.model.AuthField

class AuthValidatorTest {

    @Test
    fun emptyLoginFieldsAreRejected() {
        val errors = AuthValidator.validateLogin(
            email = "",
            password = ""
        )

        assertTrue(errors.containsKey(AuthField.EMAIL))
        assertTrue(errors.containsKey(AuthField.PASSWORD))
    }

    @Test
    fun invalidEmailIsRejected() {
        val errors = AuthValidator.validateLogin(
            email = "student.example.test",
            password = "Study1234"
        )

        assertTrue(errors.containsKey(AuthField.EMAIL))
    }

    @Test
    fun validRegistrationIsAccepted() {
        val errors = AuthValidator.validateRegistration(
            name = "Христина",
            email = "student@example.test",
            password = "Study1234",
            confirmPassword = "Study1234"
        )

        assertTrue(errors.isEmpty())
    }

    @Test
    fun passwordWithoutDigitIsRejected() {
        val errors = AuthValidator.validateRegistration(
            name = "Христина",
            email = "student@example.test",
            password = "StudyPassword",
            confirmPassword = "StudyPassword"
        )

        assertTrue(errors.containsKey(AuthField.PASSWORD))
    }

    @Test
    fun mismatchingPasswordsAreRejected() {
        val errors = AuthValidator.validateRegistration(
            name = "Христина",
            email = "student@example.test",
            password = "Study1234",
            confirmPassword = "Study5678"
        )

        assertTrue(
            errors.containsKey(AuthField.CONFIRM_PASSWORD)
        )
    }
}