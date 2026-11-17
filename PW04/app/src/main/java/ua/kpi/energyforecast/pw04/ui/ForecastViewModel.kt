package ua.kpi.energyforecast.pw04.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import ua.kpi.energyforecast.pw04.data.*
import ua.kpi.energyforecast.pw04.logic.*
import ua.kpi.energyforecast.pw04.model.*

data class ForecastUiState(
    val telemetry: TelemetryState = TelemetryState(),
    val query: SiteQuery = SiteQuery(),
    val visible: List<SiteReading> = emptyList(),
    val selectedId: String? = null,
    val selectionRevision: Long = 0
)

private data class SiteSelection(val id: String? = null, val revision: Long = 0)

@OptIn(ExperimentalCoroutinesApi::class)
class ForecastViewModel @JvmOverloads constructor(
    private val saved: SavedStateHandle,
    private val source: DemoTelemetrySource = DemoTelemetrySource(DemoSites.sites),
    private val engine: TelemetryEngine = TelemetryEngine(DemoSites.sites, source),
    private val failureAction: () -> Unit = source::simulateFailure,
    private val forecastFailureAction: () -> Unit = source::simulateForecastFailure
) : ViewModel() {
    private val query = MutableStateFlow(SiteQuery(
        search = saved["search"] ?: "",
        type = saved.get<String>("type")?.let { SiteType.valueOf(it) },
        status = saved.get<String>("status")?.let { SiteStatus.valueOf(it) },
        minPowerKw = saved["min"] ?: "", maxPowerKw = saved["max"] ?: "",
        primary = SiteSort.valueOf(saved["primary"] ?: SiteSort.NAME.name),
        secondary = SiteSort.valueOf(saved["secondary"] ?: SiteSort.POWER.name),
        descending = saved["descending"] ?: false
    ))
    // ID — збережений вибір, revision — нова явна дія, навіть якщо ID не змінився.
    private val selected = MutableStateFlow(SiteSelection(saved["selected"]))
    private val restart = MutableStateFlow(0)

    // Один гарячий потік для всіх екранів. Немає окремих генераторів для кожної картки.
    // Після відписки UI джерела зупиняються через 5 с, при поверненні запускаються знову.
    val telemetry = restart.flatMapLatest { engine.observe() }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000, 0), replay = 1)

    val uiState = combine(telemetry, query, selected) { data, filters, selection ->
        val visible = querySites(data.readings, filters)
        ForecastUiState(data, filters, visible,
            selection.id?.takeIf { key -> visible.any { it.site.id == key } }, selection.revision)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(0, 0), ForecastUiState(query = query.value))

    fun updateQuery(value: SiteQuery) {
        query.value = value
        saved["search"] = value.search
        saved["type"] = value.type?.name
        saved["status"] = value.status?.name
        saved["min"] = value.minPowerKw
        saved["max"] = value.maxPowerKw
        saved["primary"] = value.primary.name
        saved["secondary"] = value.secondary.name
        saved["descending"] = value.descending
        // Прихований фільтром об'єкт більше не залишається вибраним.
        if (uiState.value.telemetry.readings.isNotEmpty() &&
            querySites(uiState.value.telemetry.readings, value).none { it.site.id == selected.value.id }) {
            saved["selected"] = null
            selected.update { it.copy(id = null) }
        }
    }
    fun select(id: String) {
        if (uiState.value.visible.any { it.site.id == id }) {
            saved["selected"] = id
            selected.update { SiteSelection(id, it.revision + 1) }
        }
    }
    fun restart() { restart.update { it + 1 } }
    fun simulateFailure() = failureAction()
    fun simulateForecastFailure() = forecastFailureAction()
}
