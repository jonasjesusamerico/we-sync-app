package com.github.jonasjesusamerico.we_sync_app.auth.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SessionViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val repository = FakeAuthRepository()
    private val viewModel = SessionViewModel(repository)

    @Test
    fun logout_signsOutOnce() {
        viewModel.logout()
        viewModel.logout()

        assertTrue(viewModel.state.value.isSignedOut)
        assertEquals(1, repository.logoutCalls)
    }

    @Test
    fun afterHandled_aLaterSessionCanSignOutAgain() {
        viewModel.logout()
        viewModel.onSignedOutHandled()
        assertFalse(viewModel.state.value.isSignedOut)

        viewModel.logout()

        assertEquals(2, repository.logoutCalls)
    }
}
