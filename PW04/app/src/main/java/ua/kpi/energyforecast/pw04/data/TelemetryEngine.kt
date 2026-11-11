package ua.kpi.energyforecast.pw04.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import ua.kpi.energyforecast.pw04.logic.forecastDaily
import ua.kpi.energyforecast.pw04.model.*
import java.io.IOException

enum class StreamPhase { WAITING, LIVE, RETRYING, FAILED }
data class TelemetryState(
    val readings: List<SiteReading> = emptyList(),
    val phase: StreamPhase = StreamPhase.WAITING,
    val retries: Int = 0,
    val factor: Double? = null
)

private data class MeterState(
    val frame: MeterFrame? = null,
    val phase: StreamPhase = StreamPhase.WAITING,
    val retries: Int = 0
)

class TelemetryEngine(
    private val sites: List<EnergySite>,
    private val source: TelemetrySource,
    private val retryDelayMs: Long = 1000,
    private val maxRetries: Long = 3
) {
    init { require(retryDelayMs >= 0 && maxRetries >= 0) }

    fun observe(): Flow<TelemetryState> {
        // retryWhen стоїть ДО shareIn у ViewModel: винятки відновлюють саме джерело.
        val meter = flow {
            var last: MeterFrame? = null
            var retries = 0
            emit(MeterState())
            source.meter()
                .filter { frame -> sites.all { site ->
                    frame.watts[site.id]?.let { it.isFinite() && it >= 0 } == true
                } }
                .map { frame -> frame.copy(watts = frame.watts.mapValues { it.value / 1000.0 }) }
                .map { frame ->
                    last = frame
                    MeterState(frame, StreamPhase.LIVE, retries)
                }
                .retryWhen { cause, attempt ->
                    if (cause is CancellationException) throw cause
                    if (cause is IOException && attempt < maxRetries) {
                        retries = (attempt + 1).toInt()
                        emit(MeterState(last, StreamPhase.RETRYING, retries))
                        delay(retryDelayMs * (attempt + 1))
                        true
                    } else false
                }
                .catch { cause ->
                    if (cause is CancellationException) throw cause
                    emit(MeterState(last, StreamPhase.FAILED, retries))
                }
                .collect { emit(it) }
        }
        // combine зберігає останній вимір при оновленні незалежної прогнозної моделі.
        return combine(meter, source.demandFactor().filter { it.isFinite() && it in 0.0..2.0 }) { state, factor ->
            val frame = state.frame
            TelemetryState(
                readings = if (frame == null) emptyList() else sites.map { site ->
                    val power = frame.watts.getValue(site.id)
                    SiteReading(site, power, forecastDaily(power, site.hoursPerDay, factor), frame.time)
                },
                phase = state.phase, retries = state.retries, factor = factor
            )
        }.catch { cause ->
            if (cause is CancellationException) throw cause
            emit(TelemetryState(phase = StreamPhase.FAILED))
        }
    }
}
