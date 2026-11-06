package ua.kpi.energyforecast.data

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

class DemoAuthRepositoryTest {

    @Test
    fun demoAccountAcceptsCorrectPassword() {
        val repository = DemoAuthRepository()

        val result = repository.login(
            email = DemoAuthRepository.DEMO_EMAIL,
            password = DemoAuthRepository.DEMO_PASSWORD
        )

        assertTrue(result is AuthResult.Success)
    }

    @Test
    fun wrongPasswordIsRejected() {
        val repository = DemoAuthRepository()

        val result = repository.login(
            email = DemoAuthRepository.DEMO_EMAIL,
            password = "Wrong1234"
        )

        assertTrue(result is AuthResult.Failure)
    }

    @Test
    fun registeredAccountCanLoginWithNormalizedEmail() {
        val repository = DemoAuthRepository()

        val registration = repository.register(
            name = "Христина",
            email = " Student@Example.Test ",
            password = "Study1234"
        )

        assertTrue(registration is AuthResult.Success)

        val login = repository.login(
            email = "student@example.test",
            password = "Study1234"
        )

        assertTrue(login is AuthResult.Success)

        val user = (login as AuthResult.Success).user

        assertEquals("Христина", user.name)
        assertEquals("student@example.test", user.email)
    }

    @Test
    fun duplicateEmailIsRejectedIgnoringCase() {
        val repository = DemoAuthRepository()

        repository.register(
            name = "Перший",
            email = "student@example.test",
            password = "Study1234"
        )

        val duplicate = repository.register(
            name = "Другий",
            email = "STUDENT@EXAMPLE.TEST",
            password = "Other1234"
        )

        assertTrue(duplicate is AuthResult.Failure)
    }
}