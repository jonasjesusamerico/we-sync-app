package com.github.jonasjesusamerico.we_sync_app.core.network

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/** Result of one API call, with the transport failures already classified. */
sealed interface ApiOutcome<out T> {
    data class Success<T>(val value: T) : ApiOutcome<T>

    /** Non-2xx response; [error] is the parsed envelope when the body had one. */
    data class HttpError(val status: Int, val error: ApiError?) : ApiOutcome<Nothing>

    /** No response: connection, timeout or I/O failure. */
    data object NetworkError : ApiOutcome<Nothing>

    /** 2xx response whose body could not be parsed. */
    data object MalformedResponse : ApiOutcome<Nothing>
}

/** Runs one Retrofit call and classifies its failures. */
inline fun <T> Json.apiCall(block: () -> T): ApiOutcome<T> = try {
    ApiOutcome.Success(block())
} catch (e: HttpException) {
    ApiOutcome.HttpError(e.code(), parseApiError(e.response()?.errorBody()?.string()))
} catch (_: IOException) {
    ApiOutcome.NetworkError
} catch (_: SerializationException) {
    ApiOutcome.MalformedResponse
}
