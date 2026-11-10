package ua.kpi.energyforecast.data.remote

import com.google.gson.JsonParseException
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import ua.kpi.energyforecast.logic.EnergyCalculator

class RemoteScenarioDataSourceTest {

    private lateinit var server: MockWebServer
    private lateinit var api: EnergyApi
    private lateinit var source: RemoteScenarioDataSource

    private val scenarioJson = """
        {
          "id": "home-office",
          "name": "Домашній офіс",
          "category": "OFFICE",
          "description": "Навчальний сценарій",
          "devices": [
            {
              "id": "laptop",
              "name": "Ноутбук",
              "powerWatts": 60.0,
              "hoursPerDay": 8.0,
              "deviceCount": 1
            },
            {
              "id": "lamp",
              "name": "Лампа",
              "powerWatts": 10.0,
              "hoursPerDay": 5.0,
              "deviceCount": 1
            }
          ]
        }
    """.trimIndent()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        api = ApiClient.create(
            baseUrl = server.url("/").toString(),
            tokenProvider = { "test-token" }
        )

        source = RemoteScenarioDataSource(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun enqueueJson(body: String, status: Int = 200) {
        server.enqueue(
            MockResponse()
                .setResponseCode(status)
                .setHeader(
                    "Content-Type",
                    "application/json; charset=utf-8"
                )
                .setBody(body)
        )
    }

    private fun catalog(scenario: String = scenarioJson): String {
        return """{"scenarios":[$scenario]}"""
    }

    @Test
    fun loadsValidatedUnicodeDataAndCalculatesForecast() = runBlocking {
        enqueueJson(catalog())

        val result = source.loadScenarios().single()

        assertEquals("Домашній офіс", result.name)
        assertEquals(2, result.devices.size)

        val forecast = EnergyCalculator.calculateTotal(
            result.devices,
            30
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

        val request = server.takeRequest()

        assertEquals("/scenarios", request.path)
        assertEquals(
            "Bearer test-token",
            request.getHeader("Authorization")
        )
    }

    @Test
    fun rejectsNegativePower() {
        enqueueJson(
            catalog(scenarioJson.replace("60.0", "-60.0"))
        )

        assertThrows(InvalidApiDataException::class.java) {
            runBlocking { source.loadScenarios() }
        }
    }

    @Test
    fun rejectsMissingScenarioName() {
        enqueueJson(
            catalog(
                scenarioJson.replace(
                    "\"name\": \"Домашній офіс\",",
                    ""
                )
            )
        )

        assertThrows(InvalidApiDataException::class.java) {
            runBlocking { source.loadScenarios() }
        }
    }

    @Test
    fun rejectsFractionalDeviceCount() {
        enqueueJson(
            catalog(
                scenarioJson.replace(
                    "\"deviceCount\": 1",
                    "\"deviceCount\": 1.5"
                )
            )
        )

        assertThrows(JsonParseException::class.java) {
            runBlocking { source.loadScenarios() }
        }
    }

    @Test
    fun rejectsNumberStoredAsString() {
        enqueueJson(
            catalog(
                scenarioJson.replace(
                    "\"powerWatts\": 60.0",
                    "\"powerWatts\": \"60.0\""
                )
            )
        )

        assertThrows(JsonParseException::class.java) {
            runBlocking { source.loadScenarios() }
        }
    }

    @Test
    fun rejectsDuplicateScenarioIds() {
        enqueueJson(
            """{"scenarios":[$scenarioJson,$scenarioJson]}"""
        )

        assertThrows(InvalidApiDataException::class.java) {
            runBlocking { source.loadScenarios() }
        }
    }

    @Test
    fun rejectsUnknownCategory() {
        enqueueJson(
            catalog(scenarioJson.replace("OFFICE", "UNKNOWN"))
        )

        assertThrows(InvalidApiDataException::class.java) {
            runBlocking { source.loadScenarios() }
        }
    }

    @Test
    fun preservesHttpErrorCode() {
        enqueueJson(
            """
            {
              "error": {
                "code": "INVALID_TOKEN",
                "message": "Помилка"
              }
            }
            """.trimIndent(),
            status = 401
        )

        val exception = assertThrows(HttpException::class.java) {
            runBlocking { source.loadScenarios() }
        }

        assertEquals(401, exception.code())
    }

    @Test
    fun rejectsDetailsForDifferentId() {
        enqueueJson(scenarioJson)

        assertThrows(InvalidApiDataException::class.java) {
            runBlocking { source.loadScenario("another-id") }
        }
    }

    @Test
    fun loginDoesNotSendExistingToken() = runBlocking {
        val response = LoginTestFixtures.response()

        enqueueJson(ApiJson.create().toJson(response))

        val loginApi = ApiClient.create(
            baseUrl = server.url("/").toString(),
            tokenProvider = { "existing-token" },
            clockSeconds = { LoginTestFixtures.NOW }
        )

        val result = loginApi.login(
            LoginRequestDto("demo@energy.test", "Demo1234")
        )

        assertEquals(response.accessToken, result.accessToken)

        val request = server.takeRequest()

        assertEquals("POST", request.method)
        assertEquals("/auth/login", request.path)
        assertNull(request.getHeader("Authorization"))

        val body = ApiJson.create().fromJson(
            request.body.readUtf8(),
            LoginRequestDto::class.java
        )

        assertEquals("demo@energy.test", body.email)
        assertEquals("Demo1234", body.password)
    }
}