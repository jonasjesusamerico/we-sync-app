package com.github.jonasjesusamerico.we_sync_app.category

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET

/** Authenticated category endpoints (`../we-sync-api/internal/category/handler.go`). */
interface CategoryApi {
    /** Global categories plus the user's own. */
    @GET("categories")
    suspend fun list(): List<CategoryDto>
}

@Serializable
data class CategoryDto(
    val id: Long,
    val name: String,
    @SerialName("is_global") val isGlobal: Boolean,
)
