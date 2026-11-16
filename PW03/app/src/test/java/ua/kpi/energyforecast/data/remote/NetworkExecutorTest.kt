package ua.kpi.energyforecast.data.remote

import java.net.ConnectException
import java.io.IOException
import java.io.EOFException
import javax.net.ssl.SSLHandshakeException
import com.google.gson.stream.MalformedJsonException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class NetworkExecutorTest {

    @Test
    fun retriesTransportIOExceptionForGetButNeverForLogin() = runTest {
        var reads = 0
        val result = NetworkExecutor().execute(ApiOperation.SCENARIOS) {
            if (++reads == 1) throw IOException("unexpected end of stream")
            "catalog"
        }
        assertEquals("catalog", result)
        assertEquals(2, reads)

        var logins = 0
        val failure = runCatching {
            NetworkExecutor().execute<Unit>(ApiOperation.LOGIN) {
                logins++
                throw IOException("unexpected end of stream")
            }
        }.exceptionOrNull()
        assertTrue(failure is IOException)
        assertEquals(1, logins)
    }

    @Test
    fun doesNotRetryParsingTlsOrOfflineFailures() = runTest {
        for (failure in listOf(NoNetworkException(), EOFException(),
            MalformedJsonException("invalid JSON"), SSLHandshakeException("invalid certificate"))) {
            var calls = 0
            val caught = runCatching {
                NetworkExecutor().execute<Unit>(ApiOperation.SCENARIOS) {
                    calls++
                    throw failure
                }
            }.exceptionOrNull()
            assertSame(failure, caught)
            assertEquals(1, calls)
        }
    }

    private fun httpError(code: Int): HttpException {
        return HttpException(
            Response.error<Any>(
                code,
                "{}".toResponseBody()
            )
        )
    }

    @Test
    fun retriesTemporaryGetFailureAndReturnsThirdResult() = runTest {
        var calls = 0
        val entries = mutableListOf<NetworkLogEntry>()

        val executor = NetworkExecutor(
            logger = { entries.add(it) }
        )

        val result = executor.execute(ApiOperation.SCENARIOS) {
            calls++

            if (calls < 3) throw httpError(503)

            "catalog"
        }

        assertEquals("catalog", result)
        assertEquals(3, calls)
        assertEquals(listOf(1, 2, 3), entries.map { it.attempt })
        assertEquals(
            listOf(503, 503, null),
            entries.map { it.httpCode }
        )
    }

    @Test
    fun stopsAfterThreeFailedGetAttempts() = runTest {
        var calls = 0

        try {
            NetworkExecutor().execute<Unit>(ApiOperation.SCENARIOS) {
                calls++
                throw httpError(503)
            }

            fail("Очікувалася помилка HTTP 503")
        } catch (error: HttpException) {
            assertEquals(503, error.code())
        }

        assertEquals(3, calls)
    }

    @Test
    fun doesNotRepeatLoginAfterConnectionFailure() = runTest {
        var calls = 0

        try {
            NetworkExecutor().execute<Unit>(ApiOperation.LOGIN) {
                calls++
                throw ConnectException()
            }

            fail("Очікувалася помилка підключення")
        } catch (error: ConnectException) {
            assertEquals(1, calls)
        }
    }

    @Test
    fun doesNotRepeatUnauthorizedGet() = runTest {
        var calls = 0

        try {
            NetworkExecutor().execute<Unit>(ApiOperation.SCENARIOS) {
                calls++
                throw httpError(401)
            }

            fail("Очікувалася помилка HTTP 401")
        } catch (error: HttpException) {
            assertEquals(401, error.code())
        }

        assertEquals(1, calls)
    }

    @Test
    fun doesNotCallApiWithoutNetwork() = runTest {
        var calls = 0

        val executor = NetworkExecutor(
            networkAvailable = { false }
        )

        try {
            executor.execute<Unit>(ApiOperation.SCENARIOS) {
                calls++
            }

            fail("Очікувалася відсутність мережі")
        } catch (error: NoNetworkException) {
            assertEquals(0, calls)
        }
    }

    @Test
    fun propagatesCancellationWithoutRetry() = runTest {
        var calls = 0

        try {
            NetworkExecutor().execute<Unit>(ApiOperation.SCENARIOS) {
                calls++
                throw CancellationException("Test cancellation")
            }

            fail("Очікувалося скасування")
        } catch (error: CancellationException) {
            assertEquals(1, calls)
        }
    }
}
