package com.github.jonasjesusamerico.we_sync_app

import android.content.Context
import com.github.jonasjesusamerico.we_sync_app.auth.AuthApi
import com.github.jonasjesusamerico.we_sync_app.auth.AuthInterceptor
import com.github.jonasjesusamerico.we_sync_app.auth.AuthRepository
import com.github.jonasjesusamerico.we_sync_app.auth.KeystoreTokenStore
import com.github.jonasjesusamerico.we_sync_app.auth.RemoteAuthRepository
import com.github.jonasjesusamerico.we_sync_app.auth.TokenAuthenticator
import com.github.jonasjesusamerico.we_sync_app.auth.TokenRefresher
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import java.util.concurrent.TimeUnit

/** Manual dependency composition, built once by [WeSyncApplication]. */
class AppContainer(context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    // Login and sign-up run bcrypt on the server and may queue under load, hence the long read timeout.
    private val publicHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val authApi: AuthApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(publicHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create()

    val tokenRefresher = TokenRefresher(authApi, KeystoreTokenStore(context))

    val authRepository: AuthRepository = RemoteAuthRepository(authApi, tokenRefresher, json)

    /** Client for the authenticated endpoints: sends the bearer token and refreshes it on 401. */
    val authenticatedHttpClient: OkHttpClient = publicHttpClient.newBuilder()
        .addInterceptor(AuthInterceptor(tokenRefresher))
        .authenticator(TokenAuthenticator(tokenRefresher))
        .build()
}
