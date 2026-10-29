package ua.kpi.energyforecast.logic

import org.junit.Assert.*
import org.junit.Test
import ua.kpi.energyforecast.model.AuthField

class AuthValidatorTest {

    @Test
    fun registrationPasswordLengthBoundariesAreEnforced() {
        listOf(7, 8, 64, 65).forEach { length ->
            // У кожному випадку є літера й цифра та однакове підтвердження: ізолюємо довжину.
            val password = "A1" + "a".repeat(length - 2)
            val errors = AuthValidator.validateRegistration(
                "Студент", "student@example.test", password, password
            )

            val expected = if (length == 7 || length == 65) {
                setOf(AuthField.PASSWORD)
            } else {
                emptySet<AuthField>()
            }
            assertEquals("Password length: $length", expected, errors.keys)
        }
    }

    @Test
    fun registrationNameLengthBoundariesApplyAfterTrimming() {
        listOf(1, 2, 60, 61).forEach { length ->
            val errors = AuthValidator.validateRegistration(
                "  " + "І".repeat(length) + "  ",
                "student@example.test", "Study1234", "Study1234"
            )

            val expected = if (length == 1 || length == 61) {
                setOf(AuthField.NAME)
            } else {
                emptySet<AuthField>()
            }
            assertEquals("Trimmed name length: $length", expected, errors.keys)
        }
    }

    @Test
    fun emailLengthBoundaryAppliesAfterTrimmingInBothForms() {
        listOf(254, 255).forEach { length ->
            // Синтетична адреса перевіряє лише чинне навчальне правило загальної довжини.
            val suffix = "@example.test"
            val email = "  " + "a".repeat(length - suffix.length) + suffix + "  "
            val expected = if (length == 255) {
                setOf(AuthField.EMAIL)
            } else {
                emptySet<AuthField>()
            }

            val loginErrors = AuthValidator.validateLogin(email, "Study1234")
            val registrationErrors = AuthValidator.validateRegistration(
                "Студент", email, "Study1234", "Study1234"
            )

            assertEquals("Login email length: $length", expected, loginErrors.keys)
            assertEquals("Registration email length: $length", expected, registrationErrors.keys)
        }
    }

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
