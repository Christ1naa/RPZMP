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
    val accuracyMeters: Float? = null,
    val provider: String? = null,
    val isMock: Boolean = false
)

fun hasLocationPermission(context: Context): Boolean = listOf(
    Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION
).any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

/** Межа Android API для детермінованих перевірок реєстрації/очищення. */
internal interface LocationPlatform {
    val fine: Boolean
    val permitted: Boolean
    val providers: List<String>
    fun enabled(provider: String): Boolean
    fun request(provider: String, listener: LocationListener)
    fun remove(listener: LocationListener)
}

private class AndroidLocationPlatform(context: Context) : LocationPlatform {
    private val context = context.applicationContext
    private val manager = this.context.getSystemService(LocationManager::class.java)
    override val fine get() = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    override val permitted get() = hasLocationPermission(context)
    override val providers get() = manager.allProviders
    override fun enabled(provider: String) = manager.isProviderEnabled(provider)
    @SuppressLint("MissingPermission")
    override fun request(provider: String, listener: LocationListener) {
        manager.requestLocationUpdates(provider, 5000L, 10f, listener, Looper.getMainLooper())
    }
    override fun remove(listener: LocationListener) = manager.removeUpdates(listener)
}

/** Лише foreground: скасування збору знімає слухача через awaitClose. */
class LocationSource internal constructor(private val platform: LocationPlatform) {
    constructor(context: Context) : this(AndroidLocationPlatform(context))

    @SuppressLint("MissingPermission") // Дозвіл перевіряється перед запитом; SecurityException обробляється.
    fun observe(): Flow<LocationState> = callbackFlow {
        if (!platform.permitted) {
            trySend(LocationState(message = "Немає дозволу на геолокацію. Мапа об'єктів доступна."))
            close()
            return@callbackFlow
        }
        val fine = platform.fine
        val candidates = if (fine) listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            else listOf(LocationManager.NETWORK_PROVIDER)
        val disabledProviders = mutableSetOf<String>()
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (!platform.permitted) {
                    trySend(LocationState(message = "Дозвіл відкликано. Надайте доступ повторно."))
                    close()
                    return
                }
                if (!location.latitude.isFinite() || location.latitude !in -90.0..90.0 ||
                    !location.longitude.isFinite() || location.longitude !in -180.0..180.0) return
                trySend(LocationState(Coordinates(location.latitude, location.longitude),
                    if (platform.fine) "Поточна позиція" else "Приблизна позиція",
                    location.accuracy.takeIf { location.hasAccuracy() && it.isFinite() && it >= 0f },
                    location.provider, androidx.core.location.LocationCompat.isMock(location)))
            }
            override fun onProviderDisabled(provider: String) {
                disabledProviders += provider
                trySend(LocationState(message = if (candidates.any { it != provider && platform.enabled(it) })
                    "Джерело $provider вимкнено. Очікування іншого джерела…"
                    else "Геолокацію вимкнено. Увімкніть GPS або мережеве визначення."))
            }
            override fun onProviderEnabled(provider: String) {
                try {
                    // Скинути minDistance-gate: після off/on перший свіжий fix потрібен
                    // навіть якщо користувач не перемістився на 10 м.
                    if (disabledProviders.remove(provider)) {
                        platform.remove(this)
                        candidates.filter { it in platform.providers }.forEach { platform.request(it, this) }
                    }
                    trySend(LocationState(message = "Очікування нової позиції від $provider…"))
                } catch (_: SecurityException) {
                    trySend(LocationState(message = "Дозвіл відкликано. Надайте доступ повторно."))
                    close()
                } catch (_: IllegalArgumentException) {
                    trySend(LocationState(message = "Сервіс геолокації недоступний."))
                    close()
                }
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
        }
        try {
            val available = candidates.filter { it in platform.providers }
            disabledProviders.addAll(available.filterNot { platform.enabled(it) })
            trySend(LocationState(message = when {
                available.isEmpty() -> "Доступного джерела геолокації немає. Для приблизної позиції потрібен мережевий провайдер."
                available.none { platform.enabled(it) } -> "Геолокацію вимкнено. Увімкніть GPS або мережеве визначення."
                else -> "Очікування свіжої позиції…"
            }))
            // Реєструємо й вимкнені джерела: onProviderEnabled відновить роботу без нового входу.
            available.forEach { platform.request(it, listener) }
        } catch (_: SecurityException) {
            trySend(LocationState(message = "Дозвіл відкликано. Надайте доступ повторно."))
            close()
        } catch (_: IllegalArgumentException) {
            trySend(LocationState(message = "Сервіс геолокації недоступний."))
            close()
        }
        awaitClose {
            // Відкликання дозволу не повинно спричиняти аварію під час очищення.
            try { platform.remove(listener) } catch (_: SecurityException) { }
        }
    }
}
