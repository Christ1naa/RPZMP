package ua.kpi.energyforecast.pw04

import android.location.Location
import android.location.LocationListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.repeatOnLifecycle
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.junit.Assert.*
import org.junit.Test
import ua.kpi.energyforecast.pw04.location.*

/** Platform adapter fake: не є доказом фізичного GPS. */
class LocationSourceTest {
    private class Platform : LocationPlatform {
        override var fine = true
        override var permitted = true
        override var providers = listOf("gps", "network")
        val enabledProviders = providers.toMutableSet()
        val requested = mutableListOf<String>()
        var listener: LocationListener? = null
        var removals = 0
        var fail = false
        override fun enabled(provider: String) = provider in enabledProviders
        override fun request(provider: String, listener: LocationListener) {
            this.listener = listener; requested += provider
            if (fail) throw SecurityException()
        }
        override fun remove(listener: LocationListener) {
            removals++
            if (fail) throw SecurityException()
        }
    }
    private suspend fun Channel<LocationState>.next() = withTimeout(3000) { receive() }
    private fun CoroutineScope.observe(p: Platform, out: Channel<LocationState>) = launch {
        LocationSource(p).observe().collect { out.send(it) }
    }
    private fun fix(lat: Double = 50.45, lon: Double = 30.52) = Location("gps").apply {
        latitude = lat; longitude = lon; accuracy = 7f
    }
    @Test fun deniedPermissionDoesNotRegister() = runBlocking {
        val p = Platform().apply { permitted = false }; val out = Channel<LocationState>(Channel.UNLIMITED)
        val job = observe(p, out)
        val state = out.next(); assertNull(state.position); assertTrue(state.message.contains("Немає дозволу"))
        job.join(); assertTrue(p.requested.isEmpty()); assertEquals(0, p.removals)
    }
    @Test fun coarseUsesOnlyNetwork() = runBlocking {
        val p = Platform().apply { fine = false }; val out = Channel<LocationState>(Channel.UNLIMITED)
        val job = observe(p, out); out.next(); assertEquals(listOf("network"), p.requested)
        p.listener!!.onLocationChanged(fix())
        val state = out.next(); assertEquals("Приблизна позиція", state.message)
        assertEquals(7f, state.accuracyMeters); assertEquals(50.45, state.position!!.latitude, 0.0)
        job.cancelAndJoin(); assertEquals(1, p.removals)
    }
    @Test fun disabledProviderRecoversAndUpdates() = runBlocking {
        val p = Platform().apply { enabledProviders.clear() }; val out = Channel<LocationState>(Channel.UNLIMITED)
        val job = observe(p, out); assertTrue(out.next().message.contains("вимкнено"))
        assertEquals(listOf("gps", "network"), p.requested)
        p.listener!!.onProviderEnabled("gps"); assertTrue(out.next().message.contains("Очікування"))
        p.listener!!.onLocationChanged(fix()); assertNotNull(out.next().position)
        p.listener!!.onLocationChanged(fix(50.46)); assertEquals(50.46, out.next().position!!.latitude, 0.0)
        p.listener!!.onProviderDisabled("gps"); assertNull(out.next().position)
        job.cancelAndJoin(); assertEquals(2, p.removals)
        assertEquals(listOf("gps", "network", "gps", "network"), p.requested)
    }
    @Test fun coarseWithoutNetworkDoesNotUseGps() = runBlocking {
        val p = Platform().apply { fine = false; providers = listOf("gps") }
        val out = Channel<LocationState>(Channel.UNLIMITED); val job = observe(p, out)
        assertTrue(out.next().message.contains("мережевий провайдер")); assertTrue(p.requested.isEmpty())
        job.cancelAndJoin()
    }
    @Test fun invalidCoordinatesAreDroppedAndAccuracyNotInvented() = runBlocking {
        val p = Platform(); val out = Channel<LocationState>(Channel.UNLIMITED); val job = observe(p, out); out.next()
        p.listener!!.onLocationChanged(fix(Double.NaN)); p.listener!!.onLocationChanged(fix(91.0))
        p.listener!!.onLocationChanged(fix(lon = 181.0)); assertTrue(out.tryReceive().isFailure)
        p.listener!!.onLocationChanged(fix().apply { removeAccuracy() })
        assertNull(out.next().accuracyMeters); job.cancelAndJoin()
    }
    @Test fun revokedPermissionClearsAndCloses() = runBlocking {
        val p = Platform(); val out = Channel<LocationState>(Channel.UNLIMITED); val job = observe(p, out); out.next()
        p.listener!!.onLocationChanged(fix()); assertNotNull(out.next().position)
        p.permitted = false; p.listener!!.onLocationChanged(fix())
        val state = out.next(); assertNull(state.position); assertTrue(state.message.contains("відкликано"))
        job.join(); assertEquals(1, p.removals)
    }
    @Test fun securityRaceAndCleanupDoNotCrash() = runBlocking {
        val p = Platform().apply { fail = true }; val out = Channel<LocationState>(Channel.UNLIMITED)
        val job = observe(p, out); out.next(); assertTrue(out.next().message.contains("відкликано"))
        job.join(); assertEquals(1, p.removals)
    }
    @Test fun lifecycleStopAndRestart() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        lateinit var registry: LifecycleRegistry
        instrumentation.runOnMainSync {
            val owner = object : LifecycleOwner { override val lifecycle get() = registry }
            registry = LifecycleRegistry(owner); registry.currentState = Lifecycle.State.CREATED
        }
        val p = Platform(); val out = Channel<LocationState>(Channel.UNLIMITED)
        val job = launch(Dispatchers.Main) {
            registry.repeatOnLifecycle(Lifecycle.State.STARTED) { LocationSource(p).observe().collect { out.send(it) } }
        }
        instrumentation.runOnMainSync { registry.currentState = Lifecycle.State.STARTED }; out.next()
        instrumentation.runOnMainSync { registry.currentState = Lifecycle.State.CREATED }
        withContext(Dispatchers.Main) { assertEquals(1, p.removals) }
        instrumentation.runOnMainSync { registry.currentState = Lifecycle.State.STARTED }; assertNull(out.next().position)
        instrumentation.runOnMainSync { registry.currentState = Lifecycle.State.DESTROYED }
        job.join(); assertEquals(2, p.removals)
    }
}
