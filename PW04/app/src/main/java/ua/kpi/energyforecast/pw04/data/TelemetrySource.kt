package ua.kpi.energyforecast.pw04.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ua.kpi.energyforecast.pw04.model.EnergySite
import ua.kpi.energyforecast.pw04.model.SiteStatus
import java.io.IOException
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sin

data class MeterFrame(val watts: Map<String, Double>, val time: Instant)

interface TelemetrySource {
    fun meter(): Flow<MeterFrame>
    fun demandFactor(): Flow<Double>
}

/** Холодні незалежні джерела: облік у Вт раз на 1 с, модель попиту раз на 1,5 с. */
class DemoTelemetrySource(private val sites: List<EnergySite>) : TelemetrySource {
    private val failNext = AtomicBoolean(false)
    fun simulateFailure() { failNext.set(true) }

    override fun meter(): Flow<MeterFrame> = flow {
        var tick = 0
        while (true) {
            delay(1000)
            if (failNext.getAndSet(false)) throw IOException("Імітація збою лічильника")
            emit(MeterFrame(sites.associate { site ->
                site.id to if (site.status == SiteStatus.MAINTENANCE) 0.0 else
                    site.ratedPowerKw * 1000.0 * (0.55 + 0.10 * sin(tick / 5.0))
            }, Instant.now()))
            tick++
        }
    }

    override fun demandFactor(): Flow<Double> = flow {
        var tick = 0
        while (true) {
            emit(1.05 + 0.05 * sin(tick / 4.0))
            delay(1500)
            tick++
        }
    }
}
