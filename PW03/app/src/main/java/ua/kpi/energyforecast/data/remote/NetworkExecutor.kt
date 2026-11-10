package ua.kpi.energyforecast.data.remote

import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import retrofit2.HttpException

enum class ApiOperation(val isRead: Boolean) {
    LOGIN(false),
    SCENARIOS(true),
    SCENARIO_DETAILS(true),
    ADMIN_SUMMARY(true)
}

data class NetworkLogEntry(
    val operation: ApiOperation,
    val attempt: Int,
    val result: String,
    val httpCode: Int?,
    val durationMillis: Long
)

/**
 * Повторює лише операції читання при тимчасових збоях.
 * Пауза delay не блокує потік і скасовується разом із корутиною.
 */
class NetworkExecutor(
    private val networkAvailable: () -> Boolean = { true },
    private val logger: (NetworkLogEntry) -> Unit = {}
) {

    suspend fun <T> execute(
        operation: ApiOperation,
        request: suspend () -> T
    ): T {
        val maxAttempts = if (operation.isRead) 3 else 1

        for (attempt in 1..maxAttempts) {
            currentCoroutineContext().ensureActive()
            val startedAt = System.nanoTime()

            try {
                if (!networkAvailable()) {
                    throw NoNetworkException()
                }

                val result = request()

                record(
                    operation = operation,
                    attempt = attempt,
                    result = "SUCCESS",
                    httpCode = null,
                    startedAt = startedAt
                )

                return result
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()

                record(
                    operation = operation,
                    attempt = attempt,
                    result = error.javaClass.simpleName,
                    httpCode = (error as? HttpException)?.code(),
                    startedAt = startedAt
                )

                if (
                    attempt == maxAttempts ||
                    !canRetry(error)
                ) {
                    throw error
                }

                // Тіло невдалого HTTP-запиту перед повтором закриваємо.
                (error as? HttpException)
                    ?.response()
                    ?.errorBody()
                    ?.close()

                delay(500L * attempt)
            }
        }

        error("Недосяжний стан виконавця запитів.")
    }

    private fun canRetry(error: Exception): Boolean {
        return when (error) {
            is HttpException ->
                error.code() in setOf(502, 503, 504)

            is SocketTimeoutException,
            is ConnectException,
            is UnknownHostException,
            is SocketException -> true

            else -> false
        }
    }

    private suspend fun record(
        operation: ApiOperation,
        attempt: Int,
        result: String,
        httpCode: Int?,
        startedAt: Long
    ) {
        val entry = NetworkLogEntry(
            operation = operation,
            attempt = attempt,
            result = result,
            httpCode = httpCode,
            durationMillis =
                (System.nanoTime() - startedAt) / 1_000_000
        )

        withContext(Dispatchers.IO) {
            logger(entry)
        }
    }
}

/** Додає спільну політику до вже створеного Retrofit API. */
class ManagedEnergyApi(
    private val delegate: EnergyApi,
    private val executor: NetworkExecutor
) : EnergyApi {

    override suspend fun login(
        request: LoginRequestDto
    ): LoginResponseDto {
        return executor.execute(ApiOperation.LOGIN) {
            delegate.login(request)
        }
    }

    override suspend fun getScenarios(): ScenarioListDto {
        return executor.execute(ApiOperation.SCENARIOS) {
            delegate.getScenarios()
        }
    }

    override suspend fun getScenario(id: String): ScenarioDto {
        return executor.execute(ApiOperation.SCENARIO_DETAILS) {
            delegate.getScenario(id)
        }
    }

    override suspend fun getAdminSummary(): AdminSummaryDto {
        return executor.execute(ApiOperation.ADMIN_SUMMARY) {
            delegate.getAdminSummary()
        }
    }
}