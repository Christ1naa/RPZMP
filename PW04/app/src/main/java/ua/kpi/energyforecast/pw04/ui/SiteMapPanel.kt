package ua.kpi.energyforecast.pw04.ui

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.net.toUri
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import ua.kpi.energyforecast.pw04.BuildConfig
import ua.kpi.energyforecast.pw04.location.*
import ua.kpi.energyforecast.pw04.model.SiteReading

@Composable
fun SiteMapPanel(readings: List<SiteReading>, selectedId: String?, onSelect: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val source = remember(context) { LocationSource(context) }
    var location by remember { mutableStateOf(LocationState()) }
    var refresh by remember { mutableIntStateOf(0) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refresh++
    }
    LaunchedEffect(lifecycle, source, refresh) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            location = LocationState()
            source.observe().collect { location = it }
        }
    }
    val playServices = remember(context) {
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth()) {
            TextButton(onClick = {
                launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                refresh++ // Повторити запит координат і коли дозвіл уже надано.
            }) { Text("Моя позиція") }
            TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }) { Text("GPS") }
            TextButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()))
            }) { Text("Дозволи") }
        }
        Text(location.message + (location.accuracyMeters?.let { " • ±${it.toInt()} м" } ?: ""), style = MaterialTheme.typography.labelSmall)
        if (!BuildConfig.MAPS_CONFIGURED || !playServices) {
            Text(if (!BuildConfig.MAPS_CONFIGURED) "Для Google Maps додайте MAPS_API_KEY у local.properties та перебудуйте застосунок."
                else "Google Play services недоступні. Перевірте пристрій та їх оновлення.")
            location.position?.let { Text("Ваша позиція: ${it.latitude}, ${it.longitude}") }
        } else {
            EnergyMap(readings, selectedId, location, refresh, onSelect, Modifier.weight(1f))
        }
    }
}

@Composable
private fun EnergyMap(
    readings: List<SiteReading>, selectedId: String?, location: LocationState, focusRequest: Int,
    onSelect: (String) -> Unit, modifier: Modifier
) {
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(50.45, 30.5234), 12f)
    }
    var loaded by remember { mutableStateOf(false) }
    var consumedFocus by remember { mutableIntStateOf(0) }
    val selected = readings.firstOrNull { it.site.id == selectedId }
    // Камера залежить від ID, а не кожного нового виміру: ручний pan не скидається щосекунди.
    LaunchedEffect(selectedId, loaded) {
        if (loaded && selected != null) camera.position = CameraPosition.fromLatLngZoom(
            LatLng(selected.site.position.latitude, selected.site.position.longitude), 14f)
    }
    LaunchedEffect(focusRequest, location.position, loaded) {
        val point = location.position
        if (loaded && point != null && focusRequest > consumedFocus) {
            camera.position = CameraPosition.fromLatLngZoom(LatLng(point.latitude, point.longitude), 14f)
            consumedFocus = focusRequest
        }
    }
    Box(modifier) {
        GoogleMap(Modifier.fillMaxSize(), cameraPositionState = camera,
            onMapLoaded = { loaded = true }, uiSettings = MapUiSettings(zoomControlsEnabled = true)) {
            readings.forEach { reading -> key(reading.site.id) {
                val site = reading.site
                val state = rememberUpdatedMarkerState(LatLng(site.position.latitude, site.position.longitude))
                Marker(state = state, title = site.name,
                    snippet = "${site.type.label} • ${site.description} • ${site.status.label} • ${number(reading.forecastDailyKwh)} кВт·год/добу",
                    icon = BitmapDescriptorFactory.defaultMarker(if (site.id == selectedId) BitmapDescriptorFactory.HUE_AZURE else BitmapDescriptorFactory.HUE_RED),
                    onClick = { onSelect(site.id); false }) // false відкриває стандартний InfoWindow.
            } }
            location.position?.let { point ->
                Marker(state = rememberUpdatedMarkerState(LatLng(point.latitude, point.longitude)), title = "Ви тут",
                    snippet = location.message,
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            }
        }
        if (!loaded) Text("Завантаження мапи… Перевірте мережу та ключ Google Maps.", modifier = Modifier.padding(8.dp))
    }
}
