package com.github.jonasjesusamerico.we_sync_app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jonasjesusamerico.we_sync_app.purchase.ListMonthResult
import com.github.jonasjesusamerico.we_sync_app.purchase.MonthSummary
import com.github.jonasjesusamerico.we_sync_app.purchase.PurchaseRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth

enum class DashboardError { NETWORK, UNEXPECTED }

data class DashboardUiState(
    val month: YearMonth,
    val isLoading: Boolean = true,
    val error: DashboardError? = null,
    /** Null while loading or on error: data of another month is never shown. */
    val summary: MonthSummary? = null,
)

class DashboardViewModel(
    private val repository: PurchaseRepository,
    currentMonth: YearMonth = YearMonth.now(),
) : ViewModel() {
    private val _state = MutableStateFlow(DashboardUiState(month = currentMonth))
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun previousMonth() = showMonth(_state.value.month.minusMonths(1))

    fun nextMonth() = showMonth(_state.value.month.plusMonths(1))

    /** Reloads the selected month (retry, or after an entry was saved). */
    fun reload() = load()

    private fun showMonth(month: YearMonth) {
        _state.update { it.copy(month = month) }
        load()
    }

    // A new load cancels the previous one, so results of another month never land here (spec 003, BR-3).
    private fun load() {
        loadJob?.cancel()
        val month = _state.value.month
        _state.update { it.copy(isLoading = true, error = null, summary = null) }
        loadJob = viewModelScope.launch {
            val result = repository.listMonth(month)
            _state.update { state ->
                if (state.month != month) return@update state
                when (result) {
                    is ListMonthResult.Success ->
                        state.copy(isLoading = false, summary = MonthSummary.from(result.items))

                    ListMonthResult.Network -> state.copy(isLoading = false, error = DashboardError.NETWORK)
                    ListMonthResult.Unexpected -> state.copy(isLoading = false, error = DashboardError.UNEXPECTED)
                }
            }
        }
    }
}
