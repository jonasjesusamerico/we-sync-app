package com.github.jonasjesusamerico.we_sync_app.auth

/**
 * Persists the refresh token. The access token is never persisted: it lives in memory in
 * [TokenRefresher] and is renewed at every app start.
 */
interface TokenStore {
    /** Returns null when there is no session or it cannot be read back. */
    fun readRefreshToken(): String?

    /** @throws TokenStoreException when the token cannot be stored securely. */
    fun saveRefreshToken(token: String)
    fun clear()
}

class TokenStoreException(cause: Throwable) : Exception("could not store the session", cause)
