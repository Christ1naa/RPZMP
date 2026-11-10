package ua.kpi.energyforecast.data.remote

import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

/**
 * Структурний JWT для тестів клієнта.
 * Він не має справжнього серверного підпису.
 */
object LoginTestFixtures {

    const val NOW = 2_000_000_000L
    const val EXPIRES_AT = NOW + 3600

    fun response(
        email: String = "demo@energy.test",
        expiresAt: Long = EXPIRES_AT
    ): LoginResponseDto {
        val encoder = Base64.getUrlEncoder().withoutPadding()

        fun encode(text: String): String {
            return encoder.encodeToString(
                text.toByteArray(StandardCharsets.UTF_8)
            )
        }

        val header = encode("""{"alg":"HS256","typ":"JWT"}""")
        val payload = encode(
            """
            {
              "sub":"demo-user",
              "email":"$email",
              "name":"Демо користувач",
              "role":"USER",
              "iat":${NOW - 60},
              "exp":$expiresAt
            }
            """.trimIndent()
        )
        val signature = encoder.encodeToString(ByteArray(32))

        return LoginResponseDto(
            accessToken = "$header.$payload.$signature",
            tokenType = "Bearer",
            expiresAt = expiresAt
        )
    }
}

class LoginResponseValidationTest {

    @Test
    fun emptySuccessfulHttpResponseIsLoggedAsInvalid() {
        checkLogin(
            body = "{}",
            expectedFailure = true
        )
    }

    @Test
    fun invalidSessionResponsesNeverLogSuccessOrRetry() {
        val valid = LoginTestFixtures.response()

        val invalidResponses = listOf(
            valid.copy(accessToken = null),
            valid.copy(tokenType = "Basic"),
            valid.copy(accessToken = "broken-token"),
            valid.copy(expiresAt = LoginTestFixtures.EXPIRES_AT + 1),
            LoginTestFixtures.response(email = "other@energy.test"),
            LoginTestFixtures.response(
                expiresAt = LoginTestFixtures.NOW - 1
            )
        )

        invalidResponses.forEach { response ->
            checkLogin(
                body = ApiJson.create().toJson(response),
                expectedFailure = true
            )
        }
    }

    @Test
    fun validResponseLogsSuccess() {
        checkLogin(
            body = ApiJson.create().toJson(
                LoginTestFixtures.response()
            ),
            expectedFailure = false
        )
    }

    private fun checkLogin(
        body: String,
        expectedFailure: Boolean
    ) = runBlocking {
        val server = MockWebServer()
        server.start()

        try {
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader(
                        "Content-Type",
                        "application/json; charset=utf-8"
                    )
                    .setBody(body)
            )

            val entries = mutableListOf<NetworkLogEntry>()

            val api = ApiClient.create(
                baseUrl = server.url("/").toString(),
                networkLogger = { entries.add(it) },
                clockSeconds = { LoginTestFixtures.NOW }
            )

            val result = runCatching {
                api.login(
                    LoginRequestDto(
                        email = "demo@energy.test",
                        password = "Demo1234"
                    )
                )
            }

            assertEquals(1, server.requestCount)
            assertEquals(1, entries.size)
            assertEquals(ApiOperation.LOGIN, entries.single().operation)
            assertEquals(1, entries.single().attempt)

            if (expectedFailure) {
                assertTrue(
                    result.exceptionOrNull() is InvalidApiDataException
                )
                assertEquals(
                    "InvalidApiDataException",
                    entries.single().result
                )
            } else {
                assertTrue(result.isSuccess)
                assertEquals(
                    LoginTestFixtures.response().accessToken,
                    result.getOrThrow().accessToken
                )
                assertEquals("SUCCESS", entries.single().result)
            }
        } finally {
            server.shutdown()
        }
    }
}