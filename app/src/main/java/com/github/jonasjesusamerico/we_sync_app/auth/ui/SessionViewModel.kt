package com.github.jonasjesusamerico.we_sync_app.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jonasjesusamerico.we_sync_app.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SessionUiState(
    val isSigningOut: Boolean = false,
    val isSignedOut: Boolean = false,
)

/** Activity-scoped: the user signs out from the navigation drawer (spec 003, D-3). */
class SessionViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(SessionUiState())
    val state: StateFlow<SessionUiState> = _state.asStateFlow()

    fun logout() {
        if (_state.value.isSigningOut || _state.value.isSignedOut) return
        _state.value = SessionUiState(isSigningOut = true)
        viewModelScope.launch {
            repository.logout()
            _state.value = SessionUiState(isSignedOut = true)
        }
    }

    /** Called once the screen navigated to Login, so a later session can sign out again. */
    fun onSignedOutHandled() {
        _state.value = SessionUiState()
    }
}
