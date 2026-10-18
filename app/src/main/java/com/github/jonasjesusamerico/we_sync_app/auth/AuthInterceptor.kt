package com.github.jonasjesusamerico.we_sync_app.auth

import okhttp3.Interceptor
import okhttp3.Response

/** Adds the current access token to requests of the authenticated client. */
class AuthInterceptor(private val refresher: TokenRefresher) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = refresher.accessToken ?: return chain.proceed(chain.request())
        return chain.proceed(chain.request().newBuilder().header(AUTHORIZATION, "$BEARER$token").build())
    }
}

internal const val AUTHORIZATION = "Authorization"
internal const val BEARER = "Bearer "
