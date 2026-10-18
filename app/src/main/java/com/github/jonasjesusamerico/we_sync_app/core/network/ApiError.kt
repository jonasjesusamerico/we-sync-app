package com.github.jonasjesusamerico.we_sync_app.core.network

import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Error envelope returned by every API endpoint: `{"code": "...", "message": "..."}`.
 * [message] is English and meant for developers; never show it to the user.
 */
@Serializable
data class ApiError(val code: String, val message: String)

/** Parses an error response body, or returns null when it is not an [ApiError] envelope. */
fun Json.parseApiError(body: String?): ApiError? {
    if (body.isNullOrBlank()) return null
    return try {
        decodeFromString<ApiError>(body)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
