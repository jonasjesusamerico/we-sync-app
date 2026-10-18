package com.github.jonasjesusamerico.we_sync_app.auth

import kotlinx.coroutines.test.runTest
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class RemoteAuthRepositoryTest {
    private val server = MockWebServer()
    private val store = FakeTokenStore()
    private lateinit var refresher: TokenRefresher
    private lateinit var repository: RemoteAuthRepository

    @Before
    fun setUp() {
        server.start()
        val api = server.authApi()
        refresher = TokenRefresher(api, store)
        repository = RemoteAuthRepository(api, refresher, testJson)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun login_success_startsSession() = runTest {
        server.enqueue(tokens(access = "access-1", refresh = "refresh-1"))

        assertEquals(AuthResult.Success, repository.login(" user@example.com ", "12345678"))
        assertEquals("refresh-1", store.refreshToken)
        assertEquals("access-1", refresher.accessToken)
        val request = server.takeRequest()
        assertEquals("/api/v1/auth/login", request.url.encodedPath)
        assertEquals("""{"email":"user@example.com","password":"12345678"}""", request.body?.utf8())
    }

    @Test
    fun login_unauthorized_isInvalidCredentials() = runTest {
        server.enqueue(apiError(401, "UNAUTHORIZED", "invalid credentials"))

        assertEquals(AuthResult.InvalidCredentials, repository.login("user@example.com", "wrong-password"))
        assertNull(store.refreshToken)
    }

    @Test
    fun login_serverErrorAndBadRequest_areUnexpected() = runTest {
        server.enqueue(apiError(500, "INTERNAL_SERVER_ERROR", "internal server error"))
        server.enqueue(apiError(400, "BAD_REQUEST", "invalid request body"))
        server.enqueue(jsonResponse(200, "not json"))

        repeat(3) { assertEquals(AuthResult.Unexpected, repository.login("user@example.com", "12345678")) }
    }

    @Test
    fun login_networkFailure_isNetwork() = runTest {
        server.close()

        assertEquals(AuthResult.Network, repository.login("user@example.com", "12345678"))
    }

    @Test
    fun register_success_signsIn() = runTest {
        server.enqueue(jsonResponse(201, """{"id":1,"email":"user@example.com"}"""))
        server.enqueue(tokens(access = "access-1", refresh = "refresh-1"))

        assertEquals(AuthResult.Success, repository.register("user@example.com", "12345678"))
        assertEquals("/api/v1/auth/register", server.takeRequest().url.encodedPath)
        assertEquals("/api/v1/auth/login", server.takeRequest().url.encodedPath)
        assertEquals("refresh-1", store.refreshToken)
    }

    @Test
    fun register_successButLoginFails_isAccountCreated() = runTest {
        server.enqueue(jsonResponse(201, """{"id":1,"email":"user@example.com"}"""))
        server.enqueue(apiError(500, "INTERNAL_SERVER_ERROR", "internal server error"))

        assertEquals(AuthResult.AccountCreated, repository.register("user@example.com", "12345678"))
        assertNull(store.refreshToken)
    }

    @Test
    fun register_conflict_isEmailTaken() = runTest {
        server.enqueue(apiError(409, "CONFLICT", "email already registered"))

        assertEquals(AuthResult.EmailTaken, repository.register("user@example.com", "12345678"))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun register_validationErrors_mapToFields() = runTest {
        server.enqueue(apiError(422, "VALIDATION_ERROR", "email is invalid"))
        server.enqueue(apiError(422, "VALIDATION_ERROR", "password must be between 8 and 72 bytes"))
        server.enqueue(apiError(422, "VALIDATION_ERROR", "something else"))

        assertEquals(AuthResult.InvalidEmail, repository.register("user@example.com", "12345678"))
        assertEquals(AuthResult.InvalidPassword, repository.register("user@example.com", "12345678"))
        assertEquals(AuthResult.Unexpected, repository.register("user@example.com", "12345678"))
    }

    @Test
    fun restoreSession_delegatesToRefresher() = runTest {
        store.refreshToken = "refresh-1"
        server.enqueue(tokens(access = "access-2", refresh = "refresh-2"))

        assertEquals(RefreshResult.Refreshed("access-2"), repository.restoreSession())
        assertEquals("refresh-2", store.refreshToken)
    }

    @Test
    fun login_whenSessionCannotBeStored_isUnexpected() = runTest {
        server.enqueue(tokens(access = "access-1", refresh = "refresh-1"))
        val api = server.authApi()
        val repository = RemoteAuthRepository(api, TokenRefresher(api, FailingTokenStore()), testJson)

        assertEquals(AuthResult.Unexpected, repository.login("user@example.com", "12345678"))
    }

    @Test
    fun restoreSession_whenSessionCannotBeStored_isFailed() = runTest {
        server.enqueue(tokens(access = "access-2", refresh = "refresh-2"))
        val api = server.authApi()
        val repository = RemoteAuthRepository(api, TokenRefresher(api, FailingTokenStore()), testJson)

        assertEquals(RefreshResult.Failed, repository.restoreSession())
    }
}
