package ua.kpi.energyforecast.data.remote

import com.google.gson.JsonParseException
import com.google.gson.stream.MalformedJsonException
import java.io.EOFException
import java.net.SocketTimeoutException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkPolicyTest {

    private fun httpError(code: Int): HttpException {
        return HttpException(
            Response.error<Any>(
                code,
                "{}".toResponseBody()
            )
        )
    }

    @Test
    fun doesNotRetryForbiddenOrMissingResource() = runTest {
        for (code in listOf(403, 404)) {
            var calls = 0

            val failure = runCatching {
                NetworkExecutor().execute<Unit>(
                    ApiOperation.SCENARIO_DETAILS
                ) {
                    calls++
                    throw httpError(code)
                }
            }.exceptionOrNull()

            assertTrue(failure is HttpException)
            assertEquals(code, (failure as HttpException).code())
            assertEquals(1, calls)
        }
    }

    @Test
    fun waitsBeforeEachRetryAndStopsAfterSuccess() = runTest {
        val attemptTimes = mutableListOf<Long>()

        val result = NetworkExecutor().execute(
            ApiOperation.SCENARIOS
        ) {
            attemptTimes.add(testScheduler.currentTime)

            if (attemptTimes.size < 3) {
                throw httpError(503)
            }

            "catalog"
        }

        assertEquals("catalog", result)

        // Віртуальний час: тест не чекає реальні 1,5 секунди.
        assertEquals(
            listOf(0L, 500L, 1500L),
            attemptTimes
        )
    }

    @Test
    fun malformedJsonIsLoggedWithoutRetry() {
        val failure = rejectedResponse(
            body = """{"scenarios":"""
        ) { api ->
            api.getScenarios()
        }

        assertTrue(
            failure is JsonParseException ||
                    failure is MalformedJsonException ||
                    failure is EOFException
        )
    }

    @Test
    fun invalidCatalogIsLoggedWithoutSuccessOrRetry() {
        val failure = rejectedResponse(
            body = """{"scenarios":null}"""
        ) { api ->
            api.getScenarios()
        }

        assertTrue(failure is InvalidApiDataException)
    }

    @Test
    fun mismatchedDetailsAreLoggedWithoutSuccessOrRetry() {
        val failure = rejectedResponse(
            body = """
                {
                  "id": "another-id",
                  "name": "Домашній офіс",
                  "category": "OFFICE",
                  "description": "Контрольний сценарій",
                  "devices": [
                    {
                      "id": "laptop",
                      "name": "Ноутбук",
                      "powerWatts": 60.0,
                      "hoursPerDay": 8.0,
                      "deviceCount": 1
                    }
                  ]
                }
            """.trimIndent()
        ) { api ->
            api.getScenario("home-office")
        }

        assertTrue(failure is InvalidApiDataException)
    }

    @Test
    fun invalidAdminSummaryIsLoggedWithoutSuccessOrRetry() {
        val failure = rejectedResponse(
            body = """
                {
                  "scenarioCount": -1,
                  "devicePositionCount": 8
                }
            """.trimIndent()
        ) { api ->
            api.getAdminSummary()
        }

        assertTrue(failure is InvalidApiDataException)
    }

    @Test
    fun messagesDistinguishCredentialsSessionAndOtherFailures() {
        val loginMessage = NetworkErrors.message(
            httpError(401),
            isLogin = true
        )
        val sessionMessage = NetworkErrors.message(httpError(401))

        assertTrue(loginMessage.contains("пароль"))
        assertTrue(sessionMessage.contains("Сесію"))
        assertNotEquals(loginMessage, sessionMessage)

        assertTrue(
            NetworkErrors.message(httpError(403)).contains("прав")
        )
        assertTrue(
            NetworkErrors.message(httpError(404)).contains("знайдено")
        )
        assertTrue(
            NetworkErrors.message(
                InvalidApiDataException("Test")
            ).contains("некоректні")
        )
        assertTrue(
            NetworkErrors.message(
                SocketTimeoutException()
            ).contains("вчасно")
        )
    }

    /**
     * Виконує запит через справжній Retrofit-клієнт.
     * Перевіряє відсутність повторів і хибного запису SUCCESS.
     */
    private fun rejectedResponse(
        body: String,
        request: suspend (EnergyApi) -> Unit
    ): Throwable = runBlocking {
        val server = MockWebServer()
        server.start()

        try {
            // Запасні відповіді дозволять завершити тест,
            // навіть якщо помилково з'являться повторні запити.
            repeat(3) {
                server.enqueue(
                    MockResponse()
                        .setResponseCode(200)
                        .setHeader(
                            "Content-Type",
                            "application/json; charset=utf-8"
                        )
                        .setBody(body)
                )
            }

            val entries = mutableListOf<NetworkLogEntry>()

            val api = ApiClient.create(
                baseUrl = server.url("/").toString(),
                networkLogger = { entries.add(it) }
            )

            val failure = runCatching {
                request(api)
            }.exceptionOrNull()

            assertNotNull("Відповідь має бути відхилена", failure)
            assertEquals(1, server.requestCount)
            assertEquals(1, entries.size)
            assertFalse(entries.any { it.result == "SUCCESS" })
            assertEquals(1, entries.single().attempt)

            requireNotNull(failure)
        } finally {
            server.shutdown()
        }
    }
}