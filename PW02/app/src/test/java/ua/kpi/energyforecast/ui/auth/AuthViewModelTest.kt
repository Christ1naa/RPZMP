package ua.kpi.energyforecast.ui.auth

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import ua.kpi.energyforecast.data.DemoAuthRepository
import ua.kpi.energyforecast.model.AuthField
import ua.kpi.energyforecast.model.AuthMode

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val mainDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        viewModel = AuthViewModel()
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun invalidSubmissionDoesNotStartLoading() =
        runTest(mainDispatcher) {
            viewModel.submit()

            assertFalse(viewModel.uiState.isLoading)
            assertNull(viewModel.uiState.user)

            assertTrue(
                viewModel.uiState.errors.containsKey(AuthField.EMAIL)
            )

            assertTrue(
                viewModel.uiState.errors.containsKey(AuthField.PASSWORD)
            )
        }

    @Test
    fun correctingNamePreservesOtherFieldErrors() =
        runTest(mainDispatcher) {
            viewModel.switchMode()
            viewModel.submit()

            viewModel.updateField(
                AuthField.NAME,
                "Христина"
            )

            val errors = viewModel.uiState.errors

            assertFalse(errors.containsKey(AuthField.NAME))
            assertTrue(errors.containsKey(AuthField.EMAIL))
            assertTrue(errors.containsKey(AuthField.PASSWORD))
            assertTrue(errors.containsKey(AuthField.CONFIRM_PASSWORD))
        }

    @Test
    fun editingEmailToAnotherInvalidValueKeepsItsError() =
        runTest(mainDispatcher) {
            viewModel.submit()

            viewModel.updateField(
                AuthField.EMAIL,
                "invalid-email"
            )

            assertTrue(
                viewModel.uiState.errors.containsKey(AuthField.EMAIL)
            )

            assertTrue(
                viewModel.uiState.errors.containsKey(AuthField.PASSWORD)
            )
        }

    @Test
    fun successfulLoginLoadsAndBlocksFormChanges() =
        runTest(mainDispatcher) {
            fillDemoLogin()

            viewModel.submit()

            assertTrue(viewModel.uiState.isLoading)
            assertNull(viewModel.uiState.user)

            // Changes must be ignored while processing.
            viewModel.updateField(
                AuthField.EMAIL,
                "changed@example.test"
            )
            viewModel.switchMode()

            assertEquals(
                DemoAuthRepository.DEMO_EMAIL,
                viewModel.uiState.email
            )
            assertEquals(AuthMode.LOGIN, viewModel.uiState.mode)

            // Start the coroutine and its simulated delay.
            runCurrent()

            advanceTimeBy(1199)

            assertTrue(viewModel.uiState.isLoading)
            assertNull(viewModel.uiState.user)

            advanceTimeBy(1)
            runCurrent()

            assertFalse(viewModel.uiState.isLoading)

            val user = requireNotNull(viewModel.uiState.user)

            assertEquals(
                DemoAuthRepository.DEMO_EMAIL,
                user.email
            )

            assertEquals("", viewModel.uiState.password)
        }

    @Test
    fun incorrectPasswordEndsLoadingAndShowsFailure() =
        runTest(mainDispatcher) {
            fillDemoLogin(password = "Wrong1234")

            viewModel.submit()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.isLoading)
            assertNull(viewModel.uiState.user)
            assertTrue(viewModel.uiState.messageIsError)
            assertFalse(viewModel.uiState.message.isNullOrBlank())
        }

    @Test
    fun repeatedRegistrationCreatesOneAccountAndAllowsLogin() =
        runTest(mainDispatcher) {
            viewModel.switchMode()

            viewModel.updateField(
                AuthField.NAME,
                "Христина"
            )
            viewModel.updateField(
                AuthField.EMAIL,
                "student@example.test"
            )
            viewModel.updateField(
                AuthField.PASSWORD,
                "Study1234"
            )
            viewModel.updateField(
                AuthField.CONFIRM_PASSWORD,
                "Study1234"
            )

            viewModel.submit()
            viewModel.submit()

            advanceUntilIdle()

            // A second registration would produce a duplicate-email error.
            assertEquals(AuthMode.LOGIN, viewModel.uiState.mode)
            assertFalse(viewModel.uiState.isLoading)
            assertFalse(viewModel.uiState.messageIsError)
            assertFalse(viewModel.uiState.message.isNullOrBlank())

            viewModel.updateField(
                AuthField.PASSWORD,
                "Study1234"
            )

            viewModel.submit()
            advanceUntilIdle()

            assertEquals(
                "student@example.test",
                viewModel.uiState.user?.email
            )

            viewModel.logout()

            assertNull(viewModel.uiState.user)
            assertEquals("", viewModel.uiState.password)
            assertEquals(AuthMode.LOGIN, viewModel.uiState.mode)
        }

    private fun fillDemoLogin(
        password: String = DemoAuthRepository.DEMO_PASSWORD
    ) {
        viewModel.updateField(
            AuthField.EMAIL,
            DemoAuthRepository.DEMO_EMAIL
        )

        viewModel.updateField(
            AuthField.PASSWORD,
            password
        )
    }
}