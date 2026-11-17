package ua.kpi.energyforecast.pw04

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import ua.kpi.energyforecast.pw04.data.*
import ua.kpi.energyforecast.pw04.location.LocationState
import ua.kpi.energyforecast.pw04.logic.*
import ua.kpi.energyforecast.pw04.model.SiteReading
import ua.kpi.energyforecast.pw04.ui.*
import java.io.File
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

/** Реальні SiteBrowser + EnergyMap; стабільні локальні виміри для відтворюваного сценарію. */
class GoogleMapBrowserTest {
    @Before fun keepScreenAwake() = keepMapsTestScreenAwake()
    @get:Rule val compose = createComposeRule()
    @NeedsMapsKey @Test fun realMarkerSelectsCardAndCombinedFiltersLeaveOneMapObject() {
        assumeTrue(BuildConfig.MAPS_CONFIGURED)
        val readings = DemoSites.sites.map { SiteReading(it, 1.0, 8.0, Instant.EPOCH) }
        val ready = AtomicBoolean(false)
        var sdk: GoogleMap? = null
        compose.setContent {
            var query by remember { mutableStateOf(SiteQuery()) }
            var selected by remember { mutableStateOf<String?>(null) }
            var revision by remember { mutableStateOf(0L) }
            val visible = querySites(readings, query)
            val select: (String) -> Unit = { selected = it; revision++ }
            MaterialTheme {
                SiteBrowser(ForecastUiState(telemetry = TelemetryState(readings, StreamPhase.LIVE),
                    query = query, visible = visible, selectedId = selected, selectionRevision = revision),
                    { query = it }, select, {}) {
                    EnergyMap(visible, selected, LocationState(), 0, select, Modifier.fillMaxSize(),
                        selectionRevision = revision, onMapAvailable = { sdk = it }, onLoaded = { ready.set(true) })
                }
            }
        }
        compose.waitUntil(45000) { ready.get() }
        val site = DemoSites.sites.first { it.id == "office-1" }
        var point = android.graphics.Point()
        compose.runOnIdle { point = sdk!!.projection.toScreenLocation(LatLng(site.position.latitude, site.position.longitude)) }
        compose.onNodeWithTag("google-map").performTouchInput { click(Offset(point.x.toFloat(), point.y - 12f)) }
        compose.waitUntil(5000) {
            compose.onAllNodes(hasTestTag("site-office-1") and isSelected()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("site-office-1").assertIsDisplayed().assertIsSelected()
        compose.onNodeWithTag("site-office-1").onChildren().filter(hasText("На мапі")).onFirst().performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(site.position.latitude, sdk!!.cameraPosition.target.latitude, 0.005)
            assertEquals(site.position.longitude, sdk!!.cameraPosition.target.longitude, 0.005)
        }
        fun scroll(text: String) = compose.onNodeWithTag("filter-list").performScrollToNode(hasText(text))
        fun choose(label: String, value: String) {
            scroll("$label: Усі"); compose.onNodeWithText("$label: Усі").performClick()
            compose.onNodeWithText(value).performClick()
        }
        compose.onNodeWithText("Фільтри").performClick()
        choose("Тип", "Офіс"); choose("Статус", "Активний")
        compose.onNodeWithTag("filter-list").performScrollToNode(hasTestTag("min-power"))
        compose.onNodeWithTag("min-power").performTextReplacement("30")
        compose.onNodeWithTag("filter-list").performScrollToNode(hasTestTag("max-power"))
        compose.onNodeWithTag("max-power").performTextReplacement("50")
        compose.onNodeWithTag("max-power").performImeAction()
        scroll("Готово"); compose.onNodeWithText("Готово").performClick()
        compose.onNodeWithTag("query-summary").assertTextContains("Знайдено: 1 з 6", substring = true)
        compose.onNodeWithTag("site-office-1").assertIsSelected().assertIsDisplayed()
        compose.onNodeWithTag("site-home-1").assertDoesNotExist()
        val done = AtomicBoolean(false)
        compose.runOnIdle {
            sdk!!.snapshot { bitmap ->
                checkNotNull(bitmap)
                val file = File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir,
                    "pw04-maps-evidence/04-filtered-map.png")
                file.parentFile!!.mkdirs()
                file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                bitmap.recycle(); done.set(true)
            }
        }
        compose.waitUntil(10000) { done.get() }
        // Скидання панелі повертає колекцію, яку отримує й справжня мапа.
        compose.onNodeWithText("Фільтри").performClick()
        scroll("Скинути"); compose.onNodeWithText("Скинути").performClick()
        scroll("Готово"); compose.onNodeWithText("Готово").performClick()
        compose.onNodeWithTag("query-summary").assertTextContains("Знайдено: 6 з 6", substring = true)
        compose.onNodeWithText("Пошук за назвою").performTextInput("Неіснуючий")
        compose.onNodeWithText("Пошук за назвою").performImeAction()
        compose.onNodeWithTag("query-summary").assertTextContains("Знайдено: 0 з 6", substring = true)
        compose.onNodeWithText("За цими умовами об'єктів немає.").assertIsDisplayed()
        // Google Maps renders asynchronously after Compose removes Marker nodes.
        // Avoid retaining the previously selected marker in the SDK snapshot.
        compose.waitForIdle()
        android.os.SystemClock.sleep(1200)
        val emptyDone = AtomicBoolean(false)
        compose.runOnIdle {
            sdk!!.snapshot { bitmap ->
                checkNotNull(bitmap)
                val file = File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir,
                    "pw04-maps-evidence/05-empty-map.png")
                file.parentFile!!.mkdirs()
                file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                bitmap.recycle(); emptyDone.set(true)
            }
        }
        compose.waitUntil(10000) { emptyDone.get() }
    }
}
