package com.github.jonasjesusamerico.we_sync_app.auth

import com.github.jonasjesusamerico.we_sync_app.core.network.parseApiError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.net.HttpURLConnection.HTTP_CONFLICT
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED

private const val HTTP_UNPROCESSABLE_ENTITY = 422

sealed interface AuthResult {
    data object Success : AuthResult

    /** Sign-up succeeded but the automatic sign-in after it did not. */
    data object AccountCreated : AuthResult
    data object InvalidCredentials : AuthResult
    data object EmailTaken : AuthResult
    data object InvalidEmail : AuthResult
    data object InvalidPassword : AuthResult
    data object Network : AuthResult
    data object Unexpected : AuthResult
}

/** Authentication operations used by the screens. Declared as an interface so ViewModel tests can fake it. */
interface AuthRepository {
    suspend fun login(email: String, password: String): AuthResult

    /** Creates the account and signs in with the same credentials. */
    suspend fun register(email: String, password: String): AuthResult

    /** Restores the stored session at app start. */
    suspend fun restoreSession(): RefreshResult
}

class RemoteAuthRepository(
    private val api: AuthApi,
    private val refresher: TokenRefresher,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AuthRepository {

    override suspend fun login(email: String, password: String): AuthResult = withContext(ioDispatcher) {
        call {
            refresher.startSession(api.login(CredentialsRequest(email.trim(), password)))
            AuthResult.Success
        }
    }

    override suspend fun register(email: String, password: String): AuthResult = withContext(ioDispatcher) {
        val registered = call {
            api.register(CredentialsRequest(email.trim(), password))
            AuthResult.Success
        }
        if (registered != AuthResult.Success) return@withContext registered

        when (login(email, password)) {
            AuthResult.Success -> AuthResult.Success
            else -> AuthResult.AccountCreated
        }
    }

    override suspend fun restoreSession(): RefreshResult = withContext(ioDispatcher) {
        refresher.refresh(staleAccessToken = null)
    }

    private inline fun call(block: () -> AuthResult): AuthResult = try {
        block()
    } catch (e: HttpException) {
        mapHttpError(e)
    } catch (_: IOException) {
        AuthResult.Network
    } catch (_: SerializationException) {
        AuthResult.Unexpected
    } catch (_: TokenStoreException) {
        AuthResult.Unexpected
    }

    private fun mapHttpError(e: HttpException): AuthResult = when (e.code()) {
        HTTP_UNAUTHORIZED -> AuthResult.InvalidCredentials
        HTTP_CONFLICT -> AuthResult.EmailTaken
        // The API reports the invalid field only in the message text ("email is invalid",
        // "password must be ..."); an unknown message falls back to the generic error.
        HTTP_UNPROCESSABLE_ENTITY -> {
            val message = json.parseApiError(e.response()?.errorBody()?.string())?.message.orEmpty()
            when {
                message.startsWith("email") -> AuthResult.InvalidEmail
                message.startsWith("password") -> AuthResult.InvalidPassword
                else -> AuthResult.Unexpected
            }
        }

        else -> AuthResult.Unexpected
    }
}
