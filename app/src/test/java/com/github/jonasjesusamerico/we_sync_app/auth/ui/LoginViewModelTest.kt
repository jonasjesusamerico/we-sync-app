package com.github.jonasjesusamerico.we_sync_app.auth.ui

import com.github.jonasjesusamerico.we_sync_app.auth.AuthResult
import com.github.jonasjesusamerico.we_sync_app.auth.FieldError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakeAuthRepository()
    private val viewModel = LoginViewModel(repository)

    @Test
    fun emptyFields_showErrorsWithoutRequest() {
        viewModel.submit("", "")

        assertEquals(FieldError.REQUIRED, viewModel.state.value.emailError)
        assertEquals(FieldError.REQUIRED, viewModel.state.value.passwordError)
        assertEquals(0, repository.loginCalls)
    }

    @Test
    fun success_isLoadingThenAuthenticated() {
        viewModel.submit("user@example.com", "12345678")
        assertTrue(viewModel.state.value.isLoading)

        repository.loginResult.complete(AuthResult.Success)

        assertFalse(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.isAuthenticated)
    }

    @Test
    fun submitWhileLoading_isIgnored() {
        viewModel.submit("user@example.com", "12345678")
        viewModel.submit("user@example.com", "12345678")

        assertEquals(1, repository.loginCalls)
    }

    @Test
    fun invalidCredentials_showsSingleMessage() {
        viewModel.submit("user@example.com", "wrong-password")
        repository.loginResult.complete(AuthResult.InvalidCredentials)

        assertEquals(AuthMessage.INVALID_CREDENTIALS, viewModel.state.value.message)
        assertEquals(null, viewModel.state.value.emailError)
        assertEquals(null, viewModel.state.value.passwordError)
    }

    @Test
    fun networkAndUnexpected_showRetryableMessages() {
        viewModel.submit("user@example.com", "12345678")
        repository.loginResult.complete(AuthResult.Network)
        assertEquals(AuthMessage.NETWORK, viewModel.state.value.message)

        repository.loginResult = kotlinx.coroutines.CompletableDeferred(AuthResult.Unexpected)
        viewModel.submit("user@example.com", "12345678")
        assertEquals(AuthMessage.UNEXPECTED, viewModel.state.value.message)
        assertEquals(2, repository.loginCalls)
    }

    @Test
    fun accountCreated_isShownAsInitialMessage() {
        val viewModel = LoginViewModel(repository, initialMessage = AuthMessage.ACCOUNT_CREATED)

        assertEquals(AuthMessage.ACCOUNT_CREATED, viewModel.state.value.message)
    }
}
