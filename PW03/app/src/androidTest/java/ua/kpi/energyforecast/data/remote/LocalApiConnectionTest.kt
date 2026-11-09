package ua.kpi.energyforecast.data.remote

import android.content.pm.PackageManager
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.HttpException
import ua.kpi.energyforecast.logic.EnergyCalculator

/**
 * Перевіряє реальне з'єднання стандартного Android Emulator
 * із навчальним FastAPI на комп'ютері.
 */
@RunWith(AndroidJUnit4::class)
class LocalApiConnectionTest {

    @Test
    fun emulatorCanLoginAndLoadProtectedScenarios() = runBlocking {
        grantLocalNetworkPermissionIfRequired()

        // Токен живе лише в межах тесту та не записується в журнал.
        var token: String? = null

        val api = ApiClient.create(
            baseUrl = ApiClient.EMULATOR_BASE_URL,
            tokenProvider = { token }
        )

        val login = api.login(
            LoginRequestDto(
                email = "demo@energy.test",
                password = "Demo1234"
            )
        )

        assertEquals("Bearer", login.tokenType)

        val accessToken = requireNotNull(login.accessToken)
        check(accessToken.isNotBlank())
        token = accessToken

        val expiresAt = requireNotNull(login.expiresAt)
        check(expiresAt > System.currentTimeMillis() / 1000)

        val source = RemoteScenarioDataSource(api)

        val scenarios = source.loadScenarios()
        assertEquals(3, scenarios.size)

        val scenario = source.loadScenario("home-office")
        assertEquals("Домашній офіс", scenario.name)
        assertEquals(2, scenario.devices.size)

        val forecast = EnergyCalculator.calculateTotal(
            devices = scenario.devices,
            forecastDays = 30
        )

        assertEquals(
            0.53,
            forecast.dailyConsumptionKwh,
            0.000001
        )

        assertEquals(
            15.90,
            forecast.periodConsumptionKwh,
            0.000001
        )

        // USER має отримати відмову від самого сервера.
        val forbidden = assertThrows(HttpException::class.java) {
            runBlocking { api.getAdminSummary() }
        }

        assertEquals(403, forbidden.code())
    }

    private fun grantLocalNetworkPermissionIfRequired() {
        if (Build.VERSION.SDK_INT < 37) return

        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val permission = "android.permission.ACCESS_LOCAL_NETWORK"

        if (
            context.checkSelfPermission(permission) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            instrumentation.uiAutomation.grantRuntimePermission(
                context.packageName,
                permission
            )
        }
    }
}