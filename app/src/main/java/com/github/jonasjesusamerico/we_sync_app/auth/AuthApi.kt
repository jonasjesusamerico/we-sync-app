package com.github.jonasjesusamerico.we_sync_app.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/** Public auth endpoints of the API (`../we-sync-api/internal/auth/handler.go`). */
interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body body: CredentialsRequest): RegisterResponse

    @POST("auth/login")
    suspend fun login(@Body body: CredentialsRequest): TokenResponse

    /** Blocking call: it runs on OkHttp's thread inside [TokenAuthenticator] and on an IO thread at startup. */
    @POST("auth/refresh")
    fun refresh(@Body body: RefreshRequest): Call<TokenResponse>

    /** Revokes the refresh token; `204` also for unknown or already revoked tokens. */
    @POST("auth/logout")
    suspend fun logout(@Body body: RefreshRequest)
}

@Serializable
data class CredentialsRequest(val email: String, val password: String)

@Serializable
data class RegisterResponse(val id: Long, val email: String)

@Serializable
data class RefreshRequest(@SerialName("refresh_token") val refreshToken: String)

@Serializable
data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("token_type") val tokenType: String,
) {
    // Keeps tokens out of logs and crash reports if the object is ever printed.
    override fun toString(): String = "TokenResponse(tokenType=$tokenType)"
}
