package ua.kpi.energyforecast.pw04.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import ua.kpi.energyforecast.pw04.model.Coordinates

data class LocationState(
    val position: Coordinates? = null,
    val message: String = "Місцезнаходження ще не отримано.",
    val accuracyMeters: Float? = null
)

fun hasLocationPermission(context: Context): Boolean = listOf(
    Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
).any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

/** Лише foreground. callbackFlow знімає слухача при STOP/відписці/відкликанні дозволу. */
class LocationSource(context: Context) {
    private val context = context.applicationContext
    private val manager = this.context.getSystemService(LocationManager::class.java)

    @SuppressLint("MissingPermission") // Дозвіл перевіряється перед запитом; SecurityException обробляється.
    fun observe(): Flow<LocationState> = callbackFlow {
        if (!hasLocationPermission(context)) {
            trySend(LocationState(message = "Немає дозволу на геолокацію. Мапа об'єктів доступна."))
            close()
            return@callbackFlow
        }
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val candidates = if (fine) listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            else listOf(LocationManager.NETWORK_PROVIDER)
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (!location.latitude.isFinite() || !location.longitude.isFinite()) return
                trySend(LocationState(Coordinates(location.latitude, location.longitude),
                    if (fine) "Поточна позиція" else "Приблизна позиція", location.accuracy))
            }
            override fun onProviderDisabled(provider: String) {
                trySend(LocationState(message = "Джерело $provider вимкнено. Увімкніть геолокацію."))
            }
            override fun onProviderEnabled(provider: String) {
                trySend(LocationState(message = "Очікування нової позиції від $provider…"))
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        try {
            val enabled = candidates.filter { manager.allProviders.contains(it) && manager.isProviderEnabled(it) }
            if (enabled.isEmpty()) {
                trySend(LocationState(message = "GPS/мережеве визначення вимкнено або недоступне. Увімкніть та повторіть."))
            } else {
                trySend(LocationState(message = "Очікування свіжої позиції…"))
                enabled.forEach { provider ->
                    // Мінімум 5 с і 10 м обмежують витрати батареї; старі lastKnown не видаємо за поточні.
                    manager.requestLocationUpdates(provider, 5000L, 10f, listener, Looper.getMainLooper())
                }
            }
        } catch (_: SecurityException) {
            trySend(LocationState(message = "Дозвіл відкликано. Надайте доступ повторно."))
        } catch (_: IllegalArgumentException) {
            trySend(LocationState(message = "Сервіс геолокації недоступний."))
        }
        awaitClose { manager.removeUpdates(listener) }
    }
}
