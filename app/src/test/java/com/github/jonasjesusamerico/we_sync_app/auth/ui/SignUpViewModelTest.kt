package com.github.jonasjesusamerico.we_sync_app.auth.ui

import com.github.jonasjesusamerico.we_sync_app.auth.AuthResult
import com.github.jonasjesusamerico.we_sync_app.auth.FieldError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SignUpViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakeAuthRepository()
    private val viewModel = SignUpViewModel(repository)

    @Test
    fun invalidInput_showsErrorsWithoutRequest() {
        viewModel.submit("user@example", "1234567", "different")

        val state = viewModel.state.value
        assertEquals(FieldError.INVALID_EMAIL, state.emailError)
        assertEquals(FieldError.PASSWORD_LENGTH, state.passwordError)
        assertEquals(FieldError.PASSWORD_MISMATCH, state.confirmationError)
        assertEquals(0, repository.registerCalls)
    }

    @Test
    fun success_isAuthenticated() {
        submitValid()
        assertTrue(viewModel.state.value.isLoading)

        repository.registerResult.complete(AuthResult.Success)

        assertTrue(viewModel.state.value.isAuthenticated)
    }

    @Test
    fun submitWhileLoading_isIgnored() {
        submitValid()
        submitValid()

        assertEquals(1, repository.registerCalls)
    }

    @Test
    fun accountCreatedWithoutSignIn_goesToLogin() {
        submitValid()
        repository.registerResult.complete(AuthResult.AccountCreated)

        assertTrue(viewModel.state.value.isAccountCreated)
    }

    @Test
    fun emailTaken_showsMessage() {
        submitValid()
        repository.registerResult.complete(AuthResult.EmailTaken)

        assertEquals(AuthMessage.EMAIL_TAKEN, viewModel.state.value.message)
    }

    @Test
    fun apiValidationErrors_mapToFields() {
        submitValid()
        repository.registerResult.complete(AuthResult.InvalidPassword)

        assertEquals(FieldError.PASSWORD_LENGTH, viewModel.state.value.passwordError)
    }

    private fun submitValid() = viewModel.submit("user@example.com", "12345678", "12345678")
}
