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

data class SignUpUiState(
    val emailError: FieldError? = null,
    val passwordError: FieldError? = null,
    val confirmationError: FieldError? = null,
    val message: AuthMessage? = null,
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    /** Account created but automatic sign-in failed: go to Login with the e-mail pre-filled. */
    val isAccountCreated: Boolean = false,
)

class SignUpViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(SignUpUiState())
    val state: StateFlow<SignUpUiState> = _state.asStateFlow()

    fun submit(email: String, password: String, confirmation: String) {
        if (_state.value.isLoading) return
        val errors = CredentialsValidator.validateSignUp(email, password, confirmation)
        _state.value = SignUpUiState(
            emailError = errors.email,
            passwordError = errors.password,
            confirmationError = errors.confirmation,
        )
        if (!errors.isValid) return

        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = repository.register(email, password)
            _state.update { it.toResultState(result) }
        }
    }

    private fun SignUpUiState.toResultState(result: AuthResult) = when (result) {
        AuthResult.Success -> copy(isLoading = false, isAuthenticated = true)
        AuthResult.AccountCreated -> copy(isLoading = false, isAccountCreated = true)
        AuthResult.EmailTaken -> copy(isLoading = false, message = AuthMessage.EMAIL_TAKEN)
        AuthResult.InvalidEmail -> copy(isLoading = false, emailError = FieldError.INVALID_EMAIL)
        AuthResult.InvalidPassword -> copy(isLoading = false, passwordError = FieldError.PASSWORD_LENGTH)
        AuthResult.Network -> copy(isLoading = false, message = AuthMessage.NETWORK)
        AuthResult.InvalidCredentials, AuthResult.Unexpected ->
            copy(isLoading = false, message = AuthMessage.UNEXPECTED)
    }
}
