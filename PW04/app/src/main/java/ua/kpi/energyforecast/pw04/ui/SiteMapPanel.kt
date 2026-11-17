package ua.kpi.energyforecast.pw04.ui

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.delay
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
fun SiteMapPanel(readings: List<SiteReading>, selectedId: String?, onSelect: (String) -> Unit, selectionRevision: Long = 0) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val source = remember(context) { LocationSource(context) }
    var location by remember { mutableStateOf(LocationState()) }
    var refresh by remember { mutableIntStateOf(0) }
    var userFocusSelectionRevision by remember { mutableLongStateOf(0) }
    var mapGeneration by remember { mutableIntStateOf(0) }
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
    val mapAvailable = BuildConfig.MAPS_CONFIGURED && playServices
    Column(if (mapAvailable) Modifier.fillMaxSize() else Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        FlowRow(Modifier.fillMaxWidth()) {
            TextButton(onClick = {
                userFocusSelectionRevision = selectionRevision
                launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                refresh++ // Повторити запит координат і коли дозвіл уже надано.
            }) { Text("Моя позиція") }
            TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }) { Text("GPS") }
            TextButton(onClick = {
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()))
            }) { Text("Дозволи") }
        }
        Text(location.message + (location.accuracyMeters?.let { " • ±${it.toInt()} м" } ?: "") +
            (if (location.isMock) " • тестова позиція" else ""), style = MaterialTheme.typography.labelSmall, modifier = Modifier.testTag("location-status"))
        if (!mapAvailable) {
            Text(if (!BuildConfig.MAPS_CONFIGURED) "Для Google Maps додайте MAPS_API_KEY у local.properties та перебудуйте застосунок."
                else "Google Play services недоступні. Перевірте пристрій та їх оновлення.")
            location.position?.let { Text("Ваша позиція: ${it.latitude}, ${it.longitude}") }
        } else {
            key(mapGeneration) {
                EnergyMap(readings, selectedId, location, refresh, onSelect, Modifier.weight(1f),
                    selectionRevision = selectionRevision, userFocusSelectionRevision = userFocusSelectionRevision,
                    onRetry = { mapGeneration++ })
            }
        }
    }
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
internal fun EnergyMap(
    readings: List<SiteReading>, selectedId: String?, location: LocationState, focusRequest: Int,
    onSelect: (String) -> Unit, modifier: Modifier,
    selectionRevision: Long = 0,
    userFocusSelectionRevision: Long = 0,
    onRetry: () -> Unit = {},
    onMapAvailable: ((com.google.android.gms.maps.GoogleMap) -> Unit)? = null,
    onLoaded: (() -> Unit)? = null,
    initialCameraPosition: CameraPosition = CameraPosition.fromLatLngZoom(LatLng(50.45, 30.5234), 12f)
) {
    val camera = rememberCameraPositionState {
        position = initialCameraPosition
    }
    var loaded by remember { mutableStateOf(false) }
    var loadTimedOut by remember { mutableStateOf(false) }
    var consumedFocus by remember { mutableIntStateOf(0) }
    SiteFocusEffect(readings, selectedId, selectionRevision, loaded) { point ->
        camera.position = CameraPosition.fromLatLngZoom(LatLng(point.latitude, point.longitude), 14f)
    }
    LaunchedEffect(loaded) {
        if (!loaded) { delay(20_000); loadTimedOut = true }
    }
    LaunchedEffect(focusRequest, location.position, loaded, selectionRevision) {
        val point = location.position
        if (loaded && point != null && focusRequest > consumedFocus) {
            // Пізня GPS-відповідь не перекриває новішу явну дію вибору об'єкта.
            if (selectionRevision == userFocusSelectionRevision) {
                camera.position = CameraPosition.fromLatLngZoom(LatLng(point.latitude, point.longitude), 14f)
            }
            consumedFocus = focusRequest
        }
    }
    BoxWithConstraints(modifier) {
        // Лишаємо місце над центром камери для багаторядкового InfoWindow у короткому слоті.
        val focusPadding = PaddingValues(top = minOf(96.dp, maxHeight * 0.4f))
        GoogleMap(Modifier.fillMaxSize().testTag("google-map"), cameraPositionState = camera, contentPadding = focusPadding,
            onMapLoaded = { loaded = true; loadTimedOut = false; onLoaded?.invoke() }, uiSettings = MapUiSettings(zoomControlsEnabled = true)) {
            // Observer не встановлює listeners/properties; SDK лишається під керуванням Compose.
            if (onMapAvailable != null) MapEffect(Unit) { sdk -> onMapAvailable(sdk) }
            siteMarkers(readings, selectedId).forEach { marker -> key(marker.id) {
                val state = rememberUpdatedMarkerState(LatLng(marker.position.latitude, marker.position.longitude))
                MarkerInfoWindow(state = state, title = marker.title, snippet = marker.snippet,
                    contentDescription = "${marker.title}. ${marker.snippet}",
                    icon = BitmapDescriptorFactory.defaultMarker(if (marker.isSelected) BitmapDescriptorFactory.HUE_AZURE else BitmapDescriptorFactory.HUE_RED),
                    onClick = { onSelect(marker.id); false }) {
                    Surface(color = androidx.compose.ui.graphics.Color.White) { SiteInfoWindow(marker) }
                }
            } }
            location.position?.let { point ->
                Marker(state = rememberUpdatedMarkerState(LatLng(point.latitude, point.longitude)), title = "Ви тут",
                    snippet = location.message,
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            }
        }
        if (!loaded) Column(Modifier.padding(8.dp).testTag("map-load-status")) {
            Text(if (loadTimedOut) "Мапа не завантажилася. Перевірте ключ, мережу та доступ до Maps SDK."
                else "Завантаження мапи… Перевірте мережу та ключ Google Maps.")
            if (loadTimedOut) Button(onClick = onRetry) { Text("Повторити мапу") }
        }
    }
}

/** SDK відображає Compose-вміст як bitmap; стандартний snippet обрізав опис. */
@Composable
internal fun SiteInfoWindow(marker: SiteMarkerPresentation) {
    Column(Modifier.widthIn(max = 240.dp).padding(8.dp)) {
        Text(marker.title, style = MaterialTheme.typography.titleSmall, color = androidx.compose.ui.graphics.Color.Black)
        Text(marker.snippet, style = MaterialTheme.typography.bodySmall, color = androidx.compose.ui.graphics.Color.Black,
            modifier = Modifier.testTag("marker-info-details"))
    }
}
