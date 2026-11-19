package ua.kpi.energyforecast.pw05.data

import android.hardware.*
import android.os.Handler
import android.os.Looper
import ua.kpi.energyforecast.pw05.domain.SensorKind

class AndroidSensorBackend(private val manager: SensorManager) : SensorBackend {
    private fun type(kind: SensorKind) = when (kind) {
        SensorKind.ACCELEROMETER -> Sensor.TYPE_ACCELEROMETER
        SensorKind.GYROSCOPE -> Sensor.TYPE_GYROSCOPE
        SensorKind.LIGHT -> Sensor.TYPE_LIGHT
    }
    override fun available(kind: SensorKind) = manager.getDefaultSensor(type(kind)) != null
    override fun register(kind: SensorKind, onReading: (Long, FloatArray) -> Unit): (() -> Unit)? {
        val sensor = manager.getDefaultSensor(type(kind)) ?: return null
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // SensorEvent повторно використовує масив: передаємо власну копію.
                onReading(event.timestamp, event.values.copyOf())
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        return try {
            if (manager.registerListener(listener, sensor, 100_000, Handler(Looper.getMainLooper()))) {
                { manager.unregisterListener(listener) }
            } else null
        } catch (_: SecurityException) { null }
    }
}
