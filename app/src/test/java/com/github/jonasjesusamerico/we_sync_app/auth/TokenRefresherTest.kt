package com.github.jonasjesusamerico.we_sync_app.auth

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class TokenRefresherTest {
    private val server = MockWebServer()
    private val store = FakeTokenStore()
    private lateinit var refresher: TokenRefresher

    @Before
    fun setUp() {
        server.start()
        refresher = TokenRefresher(server.authApi(), store)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun refresh_withoutStoredToken_returnsNoSessionWithoutCallingApi() {
        assertEquals(RefreshResult.NoSession, refresher.refresh(null))
        assertEquals(0, server.requestCount)
    }

    @Test
    fun refresh_success_storesRotatedToken() {
        store.refreshToken = "refresh-1"
        server.enqueue(tokens(access = "access-2", refresh = "refresh-2"))

        assertEquals(RefreshResult.Refreshed("access-2"), refresher.refresh(null))
        assertEquals("refresh-2", store.refreshToken)
        assertEquals("access-2", refresher.accessToken)
        val request = server.takeRequest()
        assertEquals("/api/v1/auth/refresh", request.url.encodedPath)
        assertEquals("""{"refresh_token":"refresh-1"}""", request.body?.utf8())
    }

    @Test
    fun refresh_rejected_clearsSession() {
        store.refreshToken = "refresh-1"
        server.enqueue(apiError(401, "UNAUTHORIZED", "invalid credentials"))

        assertEquals(RefreshResult.Rejected, refresher.refresh(null))
        assertNull(store.refreshToken)
        assertNull(refresher.accessToken)
    }

    @Test
    fun refresh_serverError_keepsSession() {
        store.refreshToken = "refresh-1"
        server.enqueue(apiError(500, "INTERNAL_SERVER_ERROR", "internal server error"))

        assertEquals(RefreshResult.Failed, refresher.refresh(null))
        assertEquals("refresh-1", store.refreshToken)
    }

    @Test
    fun refresh_networkFailure_keepsSession() {
        store.refreshToken = "refresh-1"
        server.close()

        assertEquals(RefreshResult.Failed, refresher.refresh(null))
        assertEquals("refresh-1", store.refreshToken)
    }

    @Test
    fun concurrentUnauthorizedCalls_triggerASingleRefresh() {
        store.refreshToken = "refresh-1"
        refresher.startSession(TokenResponse("access-old", "refresh-1", "Bearer"))
        val refreshCalls = AtomicInteger()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.url.encodedPath == "/api/v1/auth/refresh" -> {
                    refreshCalls.incrementAndGet()
                    MockResponse.Builder()
                        .code(200)
                        .body("""{"access_token":"access-new","refresh_token":"refresh-2","token_type":"Bearer"}""")
                        .headersDelay(200, TimeUnit.MILLISECONDS)
                        .build()
                }

                request.headers["Authorization"] == "Bearer access-new" -> MockResponse.Builder().code(200).build()
                else -> apiError(401, "UNAUTHORIZED", "invalid authentication token")
            }
        }
        val client = authenticatedClient()

        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        val calls = List(2) {
            executor.submit<Int> {
                start.await()
                client.newCall(Request.Builder().url(server.url("/api/v1/purchases")).build()).execute()
                    .use { it.code }
            }
        }
        start.countDown()

        assertEquals(listOf(200, 200), calls.map { it.get(5, TimeUnit.SECONDS) })
        assertEquals(1, refreshCalls.get())
        assertEquals("refresh-2", store.refreshToken)
        executor.shutdown()
    }

    @Test
    fun unauthorizedCall_withRejectedRefresh_endsSession() {
        store.refreshToken = "refresh-1"
        refresher.startSession(TokenResponse("access-old", "refresh-1", "Bearer"))
        server.enqueue(apiError(401, "UNAUTHORIZED", "invalid authentication token"))
        server.enqueue(apiError(401, "UNAUTHORIZED", "invalid credentials"))

        val (code, signedOut) = runBlocking {
            // UNDISPATCHED subscribes before the call; signedOut has no replay.
            val event = async(start = CoroutineStart.UNDISPATCHED) {
                withTimeoutOrNull(2_000) { refresher.signedOut.first() }
            }
            val code = withContext(Dispatchers.IO) {
                authenticatedClient()
                    .newCall(Request.Builder().url(server.url("/api/v1/purchases")).build())
                    .execute().use { it.code }
            }
            code to event.await()
        }

        assertEquals(401, code)
        assertNotNull(signedOut)
        assertNull(store.refreshToken)
        assertEquals(2, server.requestCount) // original call + refresh, no retry
    }

    private fun authenticatedClient() = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(refresher))
        .authenticator(TokenAuthenticator(refresher))
        .build()
}
