package com.github.jonasjesusamerico.we_sync_app.paymentmethod

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET

/** Authenticated payment method endpoints (`../we-sync-api/internal/paymentmethod/handler.go`). */
interface PaymentMethodApi {
    /** Global payment methods plus the user's own. */
    @GET("payment-methods")
    suspend fun list(): List<PaymentMethodDto>
}

@Serializable
data class PaymentMethodDto(
    val id: Long,
    val name: String,
    @SerialName("is_global") val isGlobal: Boolean,
)
