package com.github.jonasjesusamerico.we_sync_app.auth.ui

import com.github.jonasjesusamerico.we_sync_app.auth.RefreshResult
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SessionGateViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakeAuthRepository()

    @Test
    fun restoresOnCreation() {
        val viewModel = SessionGateViewModel(repository)
        assertEquals(SessionGateState.LOADING, viewModel.state.value)

        repository.restoreResult.complete(RefreshResult.Refreshed("access"))

        assertEquals(SessionGateState.AUTHENTICATED, viewModel.state.value)
    }

    @Test
    fun noSessionOrRejected_isUnauthenticated() {
        listOf(RefreshResult.NoSession, RefreshResult.Rejected).forEach { result ->
            repository.restoreResult = CompletableDeferred(result)
            assertEquals(SessionGateState.UNAUTHENTICATED, SessionGateViewModel(repository).state.value)
        }
    }

    @Test
    fun failure_isOfflineAndRetryRestoresAgain() {
        repository.restoreResult = CompletableDeferred(RefreshResult.Failed)
        val viewModel = SessionGateViewModel(repository)
        assertEquals(SessionGateState.OFFLINE, viewModel.state.value)

        repository.restoreResult = CompletableDeferred()
        viewModel.retry()
        assertEquals(SessionGateState.LOADING, viewModel.state.value)
        viewModel.retry() // ignored while loading

        repository.restoreResult.complete(RefreshResult.Refreshed("access"))
        assertEquals(SessionGateState.AUTHENTICATED, viewModel.state.value)
        assertEquals(2, repository.restoreCalls)
    }
}
