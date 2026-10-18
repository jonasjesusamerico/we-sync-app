package com.github.jonasjesusamerico.we_sync_app.auth

import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create

internal val testJson = Json { ignoreUnknownKeys = true }

internal fun MockWebServer.authApi(client: OkHttpClient = OkHttpClient()): AuthApi = Retrofit.Builder()
    .baseUrl(url("/api/v1/"))
    .client(client)
    .addConverterFactory(testJson.asConverterFactory("application/json".toMediaType()))
    .build()
    .create()

internal fun jsonResponse(code: Int, body: String): MockResponse =
    MockResponse.Builder().code(code).addHeader("Content-Type", "application/json").body(body).build()

internal fun tokens(access: String, refresh: String) =
    jsonResponse(200, """{"access_token":"$access","refresh_token":"$refresh","token_type":"Bearer"}""")

internal fun apiError(code: Int, errorCode: String, message: String) =
    jsonResponse(code, """{"code":"$errorCode","message":"$message"}""")

internal class FakeTokenStore(var refreshToken: String? = null) : TokenStore {
    override fun readRefreshToken(): String? = refreshToken
    override fun saveRefreshToken(token: String) {
        refreshToken = token
    }

    override fun clear() {
        refreshToken = null
    }
}

internal class FailingTokenStore : TokenStore {
    override fun readRefreshToken(): String? = "refresh-1"
    override fun saveRefreshToken(token: String) = throw TokenStoreException(IllegalStateException("keystore"))
    override fun clear() = Unit
}
