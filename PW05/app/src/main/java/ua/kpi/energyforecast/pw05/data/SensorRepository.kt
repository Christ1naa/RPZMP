package ua.kpi.energyforecast.pw05.data

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.buffer
import ua.kpi.energyforecast.pw05.domain.*

interface SensorBackend {
    fun available(kind: SensorKind): Boolean
    /** Повертає дію відписки; null означає відмову реєстрації. Callback може надходити з іншого потоку. */
    fun register(kind: SensorKind, onReading: (Long, FloatArray) -> Unit): (() -> Unit)?
}
class SensorRepository(private val backend: SensorBackend) {
    fun observe(kind: SensorKind): Flow<SensorSnapshot> = callbackFlow {
        if (!backend.available(kind)) {
            trySend(SensorSnapshot(kind, SensorStatus.MISSING)); close(); return@callbackFlow
        }
        val lock = Any()
        val history = ArrayDeque<SensorReading>()
        var lastTime = 0L
        var stale: Job? = null
        var closed = false
        trySend(SensorSnapshot(kind, SensorStatus.WAITING))
        val stop = backend.register(kind) { time, values ->
            synchronized(lock) {
                if (!closed) {
                    val reading = ReadingValidator.parse(kind, time, values)
                    if (reading == null || time <= lastTime) {
                        trySend(SensorSnapshot(kind, SensorStatus.INVALID, history.toList()))
                    } else if (lastTime == 0L || time - lastTime >= 100_000_000L) {
                        // Не більше 10 оновлень/с на сенсор і 120 вимірів. Усі доступи до історії під lock.
                        lastTime = time
                        history.addLast(reading)
                        if (history.size > 120) history.removeFirst()
                        trySend(SensorSnapshot(kind, SensorStatus.LIVE, history.toList()))
                        stale?.cancel()
                        stale = launch {
                            delay(5000)
                            synchronized(lock) {
                                if (!closed && lastTime == reading.timestampNanos) trySend(SensorSnapshot(kind, SensorStatus.STALE, history.toList()))
                            }
                        }
                    }
                }
            }
        }
        if (stop == null) { trySend(SensorSnapshot(kind, SensorStatus.FAILED)); close() }
        awaitClose {
            synchronized(lock) { closed = true; stale?.cancel() }
            stop?.invoke()
        }
    }.buffer(Channel.CONFLATED)
}

