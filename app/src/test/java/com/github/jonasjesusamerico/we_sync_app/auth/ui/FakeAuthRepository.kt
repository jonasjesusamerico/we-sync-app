package com.github.jonasjesusamerico.we_sync_app.auth.ui

import com.github.jonasjesusamerico.we_sync_app.auth.AuthRepository
import com.github.jonasjesusamerico.we_sync_app.auth.AuthResult
import com.github.jonasjesusamerico.we_sync_app.auth.RefreshResult
import kotlinx.coroutines.CompletableDeferred

/** Each call suspends until the test completes the matching deferred. */
internal class FakeAuthRepository : AuthRepository {
    var loginResult = CompletableDeferred<AuthResult>()
    var registerResult = CompletableDeferred<AuthResult>()
    var restoreResult = CompletableDeferred<RefreshResult>()
    var loginCalls = 0
    var registerCalls = 0
    var restoreCalls = 0

    override suspend fun login(email: String, password: String): AuthResult {
        loginCalls++
        return loginResult.await()
    }

    override suspend fun register(email: String, password: String): AuthResult {
        registerCalls++
        return registerResult.await()
    }

    override suspend fun restoreSession(): RefreshResult {
        restoreCalls++
        return restoreResult.await()
    }
}
