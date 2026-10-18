package com.github.jonasjesusamerico.we_sync_app.home

import com.github.jonasjesusamerico.we_sync_app.auth.ui.MainDispatcherRule
import com.github.jonasjesusamerico.we_sync_app.purchase.ListMonthResult
import com.github.jonasjesusamerico.we_sync_app.purchase.purchase
import com.github.jonasjesusamerico.we_sync_app.purchase.ui.FakePurchaseRepository
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.YearMonth

class DashboardViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val october = YearMonth.of(2026, 10)
    private val repository = FakePurchaseRepository()

    @Test
    fun loadsTheCurrentMonth() {
        repository.monthResults.add(CompletableDeferred(ListMonthResult.Success(listOf(purchase(1, "12.34")))))

        val viewModel = DashboardViewModel(repository, october)

        assertEquals(listOf(october), repository.requestedMonths)
        assertEquals(BigDecimal("12.34"), viewModel.state.value.summary?.total)
    }

    @Test
    fun previousAndNextMonth_reload() {
        val viewModel = DashboardViewModel(repository, october)

        viewModel.previousMonth()
        viewModel.nextMonth()
        viewModel.nextMonth()

        assertEquals(
            listOf(october, YearMonth.of(2026, 9), october, YearMonth.of(2026, 11)),
            repository.requestedMonths,
        )
        assertEquals(YearMonth.of(2026, 11), viewModel.state.value.month)
    }

    @Test
    fun changingMonth_discardsTheResultOfThePreviousOne() {
        val octoberResult = CompletableDeferred<ListMonthResult>()
        repository.monthResults.add(octoberResult)
        val novemberResult = CompletableDeferred<ListMonthResult>()
        repository.monthResults.add(novemberResult)
        val viewModel = DashboardViewModel(repository, october)

        viewModel.nextMonth()
        octoberResult.complete(ListMonthResult.Success(listOf(purchase(1, "99.00"))))

        assertTrue(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.summary)

        novemberResult.complete(ListMonthResult.Success(listOf(purchase(2, "1.00"))))
        assertEquals(BigDecimal("1.00"), viewModel.state.value.summary?.total)
    }

    @Test
    fun error_thenRetry() {
        repository.monthResults.add(CompletableDeferred(ListMonthResult.Network))
        val viewModel = DashboardViewModel(repository, october)
        assertEquals(DashboardError.NETWORK, viewModel.state.value.error)
        assertNull(viewModel.state.value.summary)

        repository.monthResults.add(CompletableDeferred(ListMonthResult.Success(emptyList())))
        viewModel.reload()

        assertNull(viewModel.state.value.error)
        assertTrue(viewModel.state.value.summary!!.isEmpty)
        assertEquals(2, repository.requestedMonths.size)
    }
}
