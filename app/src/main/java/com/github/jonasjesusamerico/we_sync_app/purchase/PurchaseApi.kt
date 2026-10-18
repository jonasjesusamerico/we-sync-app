package com.github.jonasjesusamerico.we_sync_app.purchase

import com.github.jonasjesusamerico.we_sync_app.core.network.BigDecimalAsStringSerializer
import com.github.jonasjesusamerico.we_sync_app.core.network.LocalDateSerializer
import com.github.jonasjesusamerico.we_sync_app.category.CategoryDto
import com.github.jonasjesusamerico.we_sync_app.paymentmethod.PaymentMethodDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.math.BigDecimal
import java.time.LocalDate

/** Authenticated purchase endpoints (`../we-sync-api/internal/purchase/handler.go`). */
interface PurchaseApi {
    /**
     * Creates an entry. The response body is ignored on purpose: once the API answered 2xx the entry exists,
     * and failing on its parsing would invite a retry that creates a duplicate.
     */
    @POST("purchases")
    suspend fun create(@Body body: PurchaseRequest)

    /** One page of the user's entries of a month, newest first (`purchase_date DESC, id DESC`). */
    @GET("purchases")
    suspend fun list(
        @Query("month") month: Int,
        @Query("year") year: Int,
        @Query("page") page: Int,
        @Query("page_size") pageSize: Int,
    ): PurchasePageDto
}

/** Entry status. The form only offers [PAID] and [PENDING]; [CANCELED] comes from the API. */
@Serializable
enum class PurchaseStatus { PAID, PENDING, CANCELED }

@Serializable
data class PurchaseDto(
    val id: Long,
    @SerialName("purchase_date") @Serializable(LocalDateSerializer::class) val date: LocalDate,
    val description: String,
    val category: CategoryDto,
    @SerialName("payment_method") val paymentMethod: PaymentMethodDto,
    @Serializable(BigDecimalAsStringSerializer::class) val amount: BigDecimal,
    val status: PurchaseStatus,
)

@Serializable
data class PurchasePageDto(
    val items: List<PurchaseDto>,
    @SerialName("total_pages") val totalPages: Int,
)

@Serializable
data class PurchaseRequest(
    @SerialName("purchase_date") @Serializable(LocalDateSerializer::class) val purchaseDate: LocalDate,
    val description: String,
    @SerialName("category_id") val categoryId: Long,
    @SerialName("payment_method_id") val paymentMethodId: Long,
    @Serializable(BigDecimalAsStringSerializer::class) val amount: BigDecimal,
    val status: PurchaseStatus,
)
