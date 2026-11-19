package ua.kpi.energyforecast.pw05.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import ua.kpi.energyforecast.pw05.data.SensorRepository
import ua.kpi.energyforecast.pw05.domain.*

data class SensorSettings(val selected: Set<SensorKind> = SensorKind.entries.toSet(), val thresholds: Map<SensorKind, Float> = SensorKind.entries.associateWith { it.defaultThreshold })
data class SensorPanelState(val settings: SensorSettings = SensorSettings(), val snapshots: List<SensorSnapshot> = emptyList())
@OptIn(ExperimentalCoroutinesApi::class)
class SensorViewModel(
    repository: SensorRepository,
    initial: SensorSettings = SensorSettings(),
    private val persist: (SensorSettings) -> Unit = {},
    private val notify: (SensorKind, SensorReading, Float) -> Unit = { _, _, _ -> }
) : ViewModel() {
    private val gates = SensorKind.entries.associateWith { ThresholdGate() }
    private val processed = mutableMapOf<SensorKind, Long>()
    private val settings = MutableStateFlow(initial)
    private val snapshots = settings.map { it.selected }.distinctUntilChanged().flatMapLatest { selected ->
        if (selected.isEmpty()) flowOf(emptyList())
        else combine(selected.sortedBy { it.ordinal }.map { kind -> repository.observe(kind) }) { it.toList() }
    }
    // Без активного UI-колектора джерела негайно зупиняються; старі LIVE не відтворюються.
    val state = combine(settings, snapshots) { options, data -> SensorPanelState(options, data.filter { it.kind in options.selected }) }
        .onEach { panel ->
            panel.snapshots.filter { it.status == SensorStatus.LIVE }.forEach { snapshot ->
                snapshot.readings.lastOrNull()?.let { reading ->
                    if (processed[reading.kind] != reading.timestampNanos) {
                        processed[reading.kind] = reading.timestampNanos
                        val threshold = panel.settings.thresholds.getValue(reading.kind)
                        if (gates.getValue(reading.kind).update(reading.magnitude, threshold))
                            notify(reading.kind, reading, threshold)
                    }
                }
            } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(0, 0), SensorPanelState(initial))
    fun select(kind: SensorKind, enabled: Boolean) { settings.update { it.copy(selected = if (enabled) it.selected + kind else it.selected - kind) }; persist(settings.value) }
    fun threshold(kind: SensorKind, value: Float) {
        require(value.isFinite() && value >= 0)
        settings.update { it.copy(thresholds = it.thresholds + (kind to value)) }; persist(settings.value)
    }
}
