package com.github.jonasjesusamerico.we_sync_app.auth

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * On a 401 from an authenticated endpoint, refreshes the session once and retries the request once.
 * If the refresh is rejected, [TokenRefresher] ends the session and the 401 reaches the caller.
 */
class TokenAuthenticator(private val refresher: TokenRefresher) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.priorResponse != null) return null // already retried once

        val staleToken = response.request.header(AUTHORIZATION)?.removePrefix(BEARER)
        val result = refresher.refresh(staleToken) as? RefreshResult.Refreshed ?: return null
        return response.request.newBuilder().header(AUTHORIZATION, "$BEARER${result.accessToken}").build()
    }
}
