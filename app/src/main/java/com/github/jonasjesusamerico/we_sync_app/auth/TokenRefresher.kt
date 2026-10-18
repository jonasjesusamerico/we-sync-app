package com.github.jonasjesusamerico.we_sync_app.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED

sealed interface RefreshResult {
    data class Refreshed(val accessToken: String) : RefreshResult {
        override fun toString() = "Refreshed"
    }

    /** No refresh token stored. */
    data object NoSession : RefreshResult

    /** The API rejected the refresh token (revoked/expired); the session was cleared. */
    data object Rejected : RefreshResult

    /** Network or server failure; the stored session is kept so the user can retry. */
    data object Failed : RefreshResult
}

/**
 * Owns the session: the in-memory access token and the rotation of the refresh token.
 *
 * [refresh] is serialized because the API rotates refresh tokens: two concurrent calls with the same
 * token would make the second one fail with 401 and sign the user out.
 * All methods block; call them off the main thread.
 */
class TokenRefresher(
    private val api: AuthApi,
    private val tokenStore: TokenStore,
) {
    @Volatile
    var accessToken: String? = null
        private set

    private val _signedOut = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Emits when the session ends because it can no longer be refreshed. */
    val signedOut: SharedFlow<Unit> = _signedOut.asSharedFlow()

    @Synchronized
    fun startSession(tokens: TokenResponse) {
        tokenStore.saveRefreshToken(tokens.refreshToken)
        accessToken = tokens.accessToken
    }

    /**
     * Ends the session on this device and returns the refresh token that was stored, so the caller can
     * revoke it in the API. Serialized with [refresh], so an in-flight rotation cannot store a new token
     * after this returns. Does not emit [signedOut]: the user asked for it and the screen navigates itself.
     */
    @Synchronized
    fun clearSession(): String? {
        val refreshToken = tokenStore.readRefreshToken()
        tokenStore.clear()
        accessToken = null
        return refreshToken
    }

    /**
     * Refreshes the token pair. [staleAccessToken] is the access token the caller saw rejected; if
     * another caller already replaced it, the current token is returned without calling the API.
     */
    @Synchronized
    fun refresh(staleAccessToken: String?): RefreshResult {
        accessToken?.let { current ->
            if (current != staleAccessToken) return RefreshResult.Refreshed(current)
        }
        val refreshToken = tokenStore.readRefreshToken() ?: return endSession(RefreshResult.NoSession)

        return try {
            val response = api.refresh(RefreshRequest(refreshToken)).execute()
            val tokens = response.body()
            when {
                response.isSuccessful && tokens != null -> {
                    startSession(tokens)
                    RefreshResult.Refreshed(tokens.accessToken)
                }

                response.code() == HTTP_UNAUTHORIZED -> endSession(RefreshResult.Rejected)
                else -> RefreshResult.Failed
            }
        } catch (_: IOException) {
            RefreshResult.Failed
        } catch (_: SerializationException) {
            RefreshResult.Failed
        } catch (_: TokenStoreException) {
            RefreshResult.Failed
        }
    }

    private fun endSession(result: RefreshResult): RefreshResult {
        tokenStore.clear()
        accessToken = null
        _signedOut.tryEmit(Unit)
        return result
    }
}
