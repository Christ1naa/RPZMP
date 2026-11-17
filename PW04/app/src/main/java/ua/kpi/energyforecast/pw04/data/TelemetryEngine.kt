package ua.kpi.energyforecast.pw04.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.*
import ua.kpi.energyforecast.pw04.logic.forecastDaily
import ua.kpi.energyforecast.pw04.model.*
import java.io.IOException

enum class StreamPhase { WAITING, LIVE, RETRYING, FAILED }
enum class StreamIssue { NO_DATA, IO, SOURCE_ERROR }

data class SourceStatus(
    val phase: StreamPhase = StreamPhase.WAITING,
    val retries: Int = 0,
    val maxRetries: Int = 3,
    val issue: StreamIssue? = null
)

data class TelemetryState(
    val readings: List<SiteReading> = emptyList(),
    val phase: StreamPhase = StreamPhase.WAITING,
    val retries: Int = 0,
    val factor: Double? = null,
    val meterStatus: SourceStatus = SourceStatus(),
    val forecastStatus: SourceStatus = SourceStatus()
) {
    val isStale: Boolean get() = readings.isNotEmpty() && phase != StreamPhase.LIVE
}

private data class SourceState<T>(val value: T? = null, val status: SourceStatus)
private class NoDataException : IOException()

class TelemetryEngine(
    private val sites: List<EnergySite>,
    private val source: TelemetrySource,
    private val retryDelayMs: Long = 1000,
    private val maxRetries: Int = 3,
    private val dataTimeoutMs: Long = 5000
) {
    init {
        require(retryDelayMs in 0..60_000 && maxRetries in 0..10 && dataTimeoutMs > 0)
    }

    fun observe(): Flow<TelemetryState> {
        val meter = resilient {
            source.meter()
                .filter { frame -> sites.all { site ->
                    frame.watts[site.id]?.let { it.isFinite() && it >= 0 } == true
                } }
                .map { frame -> frame.copy(watts = frame.watts.mapValues { it.value / 1000.0 }) }
        }
        val forecast = resilient {
            source.demandFactor().filter { it.isFinite() && it in 0.0..2.0 }
        }
        // combine реагує на кожне джерело. runningFold зберігає останній ПОВНИЙ прогноз:
        // новий коефіцієнт не перераховує старий кадр лічильника під час його збою.
        return combine(meter, forecast) { measured, predicted -> measured to predicted }
            .runningFold(TelemetryState()) { previous, (measured, predicted) ->
                val phase = when {
                    measured.status.phase == StreamPhase.FAILED || predicted.status.phase == StreamPhase.FAILED -> StreamPhase.FAILED
                    measured.status.phase == StreamPhase.RETRYING || predicted.status.phase == StreamPhase.RETRYING -> StreamPhase.RETRYING
                    measured.status.phase == StreamPhase.WAITING || predicted.status.phase == StreamPhase.WAITING -> StreamPhase.WAITING
                    else -> StreamPhase.LIVE
                }
                val frame = measured.value
                val factor = predicted.value
                val live = phase == StreamPhase.LIVE && frame != null && factor != null
                TelemetryState(
                    readings = if (live) sites.map { site ->
                        val power = frame!!.watts.getValue(site.id)
                        SiteReading(site, power, forecastDaily(power, site.hoursPerDay, factor!!), frame.time)
                    } else previous.readings,
                    phase = phase,
                    retries = measured.status.retries + predicted.status.retries,
                    factor = if (live) factor else previous.factor,
                    meterStatus = measured.status,
                    forecastStatus = predicted.status
                )
            }
    }

    /** Окремий обмежений бюджет повторів на кожне джерело в межах однієї сесії. */
    private fun <T> resilient(upstream: () -> Flow<T>): Flow<SourceState<T>> = flow {
        var last: T? = null
        var retries = 0
        emit(SourceState<T>(status = SourceStatus(maxRetries = maxRetries)))
        flow { emitAll(upstream()) } // Фабрика викликається заново на кожній спробі.
            .requireTimelyData()
            .map { value ->
                last = value
                SourceState(value, SourceStatus(StreamPhase.LIVE, retries, maxRetries))
            }
            .retryWhen { cause, attempt ->
                if (cause is CancellationException) throw cause
                if (cause is IOException && attempt < maxRetries) {
                    retries = (attempt + 1).toInt()
                    emit(SourceState(last, SourceStatus(StreamPhase.RETRYING, retries, maxRetries, issue(cause))))
                    delay(retryDelayMs * (attempt + 1))
                    true
                } else false
            }
            .catch { cause ->
                if (cause is CancellationException) throw cause
                emit(SourceState(last, SourceStatus(StreamPhase.FAILED, retries, maxRetries, issue(cause))))
            }
            .collect { emit(it) }
    }

    /** Таймер починається після фільтра: невалідні кадри не маскують відсутність даних.
     * withTimeoutOrNull перетворює лише власний timeout; зовнішнє скасування поширюється.
     * Закритий потік теж помилка: ці джерела за контрактом мають працювати безперервно.
     */
    private fun <T> Flow<T>.requireTimelyData(): Flow<T> = flow {
        coroutineScope {
            val channel = Channel<T>(Channel.RENDEZVOUS)
            val producer = launch {
                try {
                    collect { channel.send(it) }
                    channel.close()
                } catch (cause: CancellationException) {
                    channel.close(cause)
                    throw cause
                } catch (cause: Exception) {
                    channel.close(cause)
                }
            }
            try {
                while (true) {
                    val result = withTimeoutOrNull(dataTimeoutMs) { channel.receiveCatching() }
                        ?: throw NoDataException()
                    result.exceptionOrNull()?.let { throw it }
                    if (result.isClosed) throw NoDataException()
                    emit(result.getOrThrow())
                }
            } finally {
                producer.cancel()
                channel.cancel()
            }
        }
    }

    private fun issue(cause: Throwable) = when (cause) {
        is NoDataException -> StreamIssue.NO_DATA
        is IOException -> StreamIssue.IO
        else -> StreamIssue.SOURCE_ERROR
    }
}
