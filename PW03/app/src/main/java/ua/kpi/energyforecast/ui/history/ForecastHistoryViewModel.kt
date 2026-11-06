package ua.kpi.energyforecast.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ua.kpi.energyforecast.data.ForecastHistoryRepository
import ua.kpi.energyforecast.model.EnergyDevice
import ua.kpi.energyforecast.model.SavedForecast

enum class HistoryPeriodFilter(val title: String) {
    ALL("Усі періоди"),
    UP_TO_WEEK("До 7 днів"),
    OVER_WEEK("Понад 7 днів")
}

enum class HistorySort(val title: String) {
    NEWEST("Спочатку нові"),
    CONSUMPTION_DESC("За споживанням за період")
}

/** Повний список історії та незалежні параметри його відображення. */
data class ForecastHistoryUiState(
    val forecasts: List<SavedForecast> = emptyList(),
    val isLoading: Boolean = true,
    val loadError: String? = null,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val operationError: String? = null,
    val message: String? = null,
    val searchQuery: String = "",
    val periodFilter: HistoryPeriodFilter = HistoryPeriodFilter.ALL,
    val sort: HistorySort = HistorySort.NEWEST
) {
    val visibleForecasts: List<SavedForecast>
        get() {
            val query = searchQuery.trim().lowercase(Locale.ROOT)

            val filtered = forecasts.filter { forecast ->
                val matchesSearch = query.isEmpty() ||
                        forecast.title.lowercase(Locale.ROOT).contains(query) ||
                        forecast.devices.any {
                            it.name.lowercase(Locale.ROOT).contains(query)
                        }

                val matchesPeriod = when (periodFilter) {
                    HistoryPeriodFilter.ALL -> true
                    HistoryPeriodFilter.UP_TO_WEEK ->
                        forecast.result.forecastDays <= 7

                    HistoryPeriodFilter.OVER_WEEK ->
                        forecast.result.forecastDays > 7
                }

                matchesSearch && matchesPeriod
            }

            return when (sort) {
                HistorySort.NEWEST -> filtered.sortedWith(
                    compareByDescending<SavedForecast> { it.createdAt }
                        .thenBy { it.id }
                )

                HistorySort.CONSUMPTION_DESC -> filtered.sortedWith(
                    compareByDescending<SavedForecast> {
                        it.result.periodConsumptionKwh
                    }.thenByDescending { it.createdAt }
                        .thenBy { it.id }
                )
            }
        }
}

/**
 * Працює з історією через Repository.
 * Деталі шукаються в повному списку, тому фільтри не приховують відкритий запис.
 */
class ForecastHistoryViewModel(
    private val repository: ForecastHistoryRepository,
    private val ownerKey: String
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(ForecastHistoryUiState())
    val uiState = mutableUiState.asStateFlow()

    private var observationJob: Job? = null

    init {
        require(ownerKey.isNotBlank())
        observeHistory()
    }

    fun retryLoad() {
        if (mutableUiState.value.loadError != null) {
            observeHistory()
        }
    }

    private fun observeHistory() {
        observationJob?.cancel()

        mutableUiState.value = mutableUiState.value.copy(
            isLoading = true,
            loadError = null
        )

        observationJob = viewModelScope.launch {
            try {
                repository.observeHistory(ownerKey).collect { forecasts ->
                    mutableUiState.value = mutableUiState.value.copy(
                        forecasts = forecasts,
                        isLoading = false,
                        loadError = null
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableUiState.value = mutableUiState.value.copy(
                    isLoading = false,
                    loadError = "Не вдалося завантажити історію."
                )
            }
        }
    }

    /**
     * Копіює параметри в момент натискання.
     * Подальше редагування поточного профілю не змінює цей прогноз.
     */
    fun saveForecast(
        title: String,
        devices: List<EnergyDevice>,
        forecastDays: Int
    ) {
        if (mutableUiState.value.isSaving) return

        val snapshot = devices.map { it.copy() }

        mutableUiState.value = mutableUiState.value.copy(
            isSaving = true,
            operationError = null,
            message = null
        )

        viewModelScope.launch {
            try {
                repository.saveForecast(
                    ownerKey = ownerKey,
                    title = title,
                    devices = snapshot,
                    forecastDays = forecastDays
                )

                mutableUiState.value = mutableUiState.value.copy(
                    message = "Прогноз збережено в історії."
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: IllegalArgumentException) {
                mutableUiState.value = mutableUiState.value.copy(
                    operationError = exception.message
                        ?: "Перевірте параметри прогнозу."
                )
            } catch (exception: Exception) {
                mutableUiState.value = mutableUiState.value.copy(
                    operationError = "Не вдалося зберегти прогноз. Спробуйте ще раз."
                )
            } finally {
                mutableUiState.value = mutableUiState.value.copy(
                    isSaving = false
                )
            }
        }
    }

    fun deleteForecast(id: String) {
        if (mutableUiState.value.isDeleting) return

        mutableUiState.value = mutableUiState.value.copy(
            isDeleting = true,
            operationError = null,
            message = null
        )

        viewModelScope.launch {
            try {
                repository.deleteForecast(ownerKey, id)

                mutableUiState.value = mutableUiState.value.copy(
                    message = "Прогноз видалено."
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                mutableUiState.value = mutableUiState.value.copy(
                    operationError = "Не вдалося видалити прогноз."
                )
            } finally {
                mutableUiState.value = mutableUiState.value.copy(
                    isDeleting = false
                )
            }
        }
    }

    fun updateSearchQuery(value: String) {
        mutableUiState.value = mutableUiState.value.copy(searchQuery = value)
    }

    fun updatePeriodFilter(value: HistoryPeriodFilter) {
        mutableUiState.value = mutableUiState.value.copy(periodFilter = value)
    }

    fun updateSort(value: HistorySort) {
        mutableUiState.value = mutableUiState.value.copy(sort = value)
    }
}