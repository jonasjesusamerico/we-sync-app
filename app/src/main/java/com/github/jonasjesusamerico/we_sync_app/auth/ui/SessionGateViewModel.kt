package com.github.jonasjesusamerico.we_sync_app.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jonasjesusamerico.we_sync_app.auth.AuthRepository
import com.github.jonasjesusamerico.we_sync_app.auth.RefreshResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SessionGateState { LOADING, AUTHENTICATED, UNAUTHENTICATED, OFFLINE }

/** Decides the first screen: restores a stored session or sends the user to Login. */
class SessionGateViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(SessionGateState.LOADING)
    val state: StateFlow<SessionGateState> = _state.asStateFlow()

    init {
        restore()
    }

    fun retry() {
        if (_state.value != SessionGateState.LOADING) restore()
    }

    private fun restore() {
        _state.value = SessionGateState.LOADING
        viewModelScope.launch {
            _state.value = when (repository.restoreSession()) {
                is RefreshResult.Refreshed -> SessionGateState.AUTHENTICATED
                RefreshResult.NoSession, RefreshResult.Rejected -> SessionGateState.UNAUTHENTICATED
                RefreshResult.Failed -> SessionGateState.OFFLINE
            }
        }
    }
}
