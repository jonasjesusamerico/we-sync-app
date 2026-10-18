package com.github.jonasjesusamerico.we_sync_app.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jonasjesusamerico.we_sync_app.auth.AuthRepository
import com.github.jonasjesusamerico.we_sync_app.auth.AuthResult
import com.github.jonasjesusamerico.we_sync_app.auth.CredentialsValidator
import com.github.jonasjesusamerico.we_sync_app.auth.FieldError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val emailError: FieldError? = null,
    val passwordError: FieldError? = null,
    val message: AuthMessage? = null,
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
)

class LoginViewModel(
    private val repository: AuthRepository,
    initialMessage: AuthMessage? = null,
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState(message = initialMessage))
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun submit(email: String, password: String) {
        if (_state.value.isLoading) return
        val errors = CredentialsValidator.validateLogin(email, password)
        _state.value = LoginUiState(emailError = errors.email, passwordError = errors.password)
        if (!errors.isValid) return

        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = repository.login(email, password)
            _state.update { it.toResultState(result) }
        }
    }

    private fun LoginUiState.toResultState(result: AuthResult) = when (result) {
        AuthResult.Success -> copy(isLoading = false, isAuthenticated = true)
        AuthResult.InvalidCredentials -> copy(isLoading = false, message = AuthMessage.INVALID_CREDENTIALS)
        // The only 422 of /auth/login is an empty field, which the validator already prevents.
        AuthResult.InvalidEmail -> copy(isLoading = false, emailError = FieldError.REQUIRED)
        AuthResult.InvalidPassword -> copy(isLoading = false, passwordError = FieldError.REQUIRED)
        AuthResult.Network -> copy(isLoading = false, message = AuthMessage.NETWORK)
        AuthResult.AccountCreated, AuthResult.EmailTaken, AuthResult.Unexpected ->
            copy(isLoading = false, message = AuthMessage.UNEXPECTED)
    }
}
