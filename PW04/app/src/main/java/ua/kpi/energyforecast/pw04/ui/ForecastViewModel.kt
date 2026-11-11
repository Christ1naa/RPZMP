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
    val selectedId: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class ForecastViewModel @JvmOverloads constructor(
    private val saved: SavedStateHandle,
    private val source: DemoTelemetrySource = DemoTelemetrySource(DemoSites.sites),
    private val engine: TelemetryEngine = TelemetryEngine(DemoSites.sites, source),
    private val failureAction: () -> Unit = source::simulateFailure
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
    private val selected = saved.getStateFlow<String?>("selected", null)
    private val restart = MutableStateFlow(0)

    // Один гарячий потік для всіх екранів. Немає окремих генераторів для кожної картки.
    // Після відписки UI джерела зупиняються через 5 с, при поверненні запускаються знову.
    val telemetry = restart.flatMapLatest { engine.observe() }
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000, 0), replay = 1)

    val uiState = combine(telemetry, query, selected) { data, filters, id ->
        val visible = querySites(data.readings, filters)
        ForecastUiState(data, filters, visible, id?.takeIf { key -> visible.any { it.site.id == key } })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), ForecastUiState(query = query.value))

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
            querySites(uiState.value.telemetry.readings, value).none { it.site.id == selected.value }) {
            saved["selected"] = null
        }
    }
    fun select(id: String) {
        if (uiState.value.visible.any { it.site.id == id }) saved["selected"] = id
    }
    fun restart() { restart.update { it + 1 } }
    fun simulateFailure() = failureAction()
}
