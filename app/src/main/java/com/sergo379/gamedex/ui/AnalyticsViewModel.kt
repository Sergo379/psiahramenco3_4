package com.sergo379.gamedex.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sergo379.gamedex.data.AnalyticsRepository
import com.sergo379.gamedex.domain.AnalyticsPoint
import com.sergo379.gamedex.domain.AnalyticsStats
import com.sergo379.gamedex.domain.StatisticsCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AnalyticsScreenState(
    val loading: Boolean = true,
    val points: List<AnalyticsPoint> = emptyList(),
    val stats: AnalyticsStats? = null,
    val fromCache: Boolean = false,
    val failedDates: Int = 0,
    val updatedAt: Long? = null,
    val error: String? = null,
)

class AnalyticsViewModel(private val days: Int, private val repository: AnalyticsRepository) : ViewModel() {
    private val _state = MutableStateFlow(AnalyticsScreenState())
    val state: StateFlow<AnalyticsScreenState> = _state.asStateFlow()
    init { refresh(false) }
    fun refresh(force: Boolean = true) = viewModelScope.launch {
        _state.value = _state.value.copy(loading = true, error = null)
        _state.value = try {
            val result = repository.load(30, force)
            val selected = StatisticsCalculator.latest(result.points, days)
            AnalyticsScreenState(
                loading = false,
                points = selected,
                stats = StatisticsCalculator.calculate(selected),
                fromCache = result.fromCache,
                failedDates = result.failedDates.count { date -> selected.any { it.date == date } },
                updatedAt = result.updatedAt,
            )
        } catch (error: Throwable) {
            _state.value.copy(loading = false, error = com.sergo379.gamedex.data.ApiErrorMapper.message(error))
        }
    }
}

@Suppress("UNCHECKED_CAST")
class AnalyticsViewModelFactory(private val days: Int, private val repository: AnalyticsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AnalyticsViewModel(days, repository) as T
}

