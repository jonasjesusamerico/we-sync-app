package com.github.jonasjesusamerico.we_sync_app.purchase

import com.github.jonasjesusamerico.we_sync_app.category.CategoryApi
import com.github.jonasjesusamerico.we_sync_app.category.CategoryDto
import com.github.jonasjesusamerico.we_sync_app.core.network.ApiOutcome
import com.github.jonasjesusamerico.we_sync_app.core.network.apiCall
import com.github.jonasjesusamerico.we_sync_app.paymentmethod.PaymentMethodApi
import com.github.jonasjesusamerico.we_sync_app.paymentmethod.PaymentMethodDto
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.text.Collator
import java.time.YearMonth
import java.util.Locale

private const val HTTP_UNPROCESSABLE_ENTITY = 422

sealed interface LoadOptionsResult {
    data class Success(
        val categories: List<CategoryDto>,
        val paymentMethods: List<PaymentMethodDto>,
    ) : LoadOptionsResult

    data object Network : LoadOptionsResult
    data object Unexpected : LoadOptionsResult
}

sealed interface CreatePurchaseResult {
    data object Success : CreatePurchaseResult

    /** The category or payment method does not exist (anymore) for this user. */
    data object NotFound : CreatePurchaseResult
    data class Invalid(val field: PurchaseField) : CreatePurchaseResult
    data object Network : CreatePurchaseResult
    data object Unexpected : CreatePurchaseResult
}

sealed interface ListMonthResult {
    /** All entries of the month, newest first. */
    data class Success(val items: List<PurchaseDto>) : ListMonthResult
    data object Network : ListMonthResult

    /** Includes months over [RemotePurchaseRepository.MAX_PAGES] pages. */
    data object Unexpected : ListMonthResult
}

/** Purchase operations used by the screens. Declared as an interface so ViewModel tests can fake it. */
interface PurchaseRepository {
    /** Categories and payment methods the user can choose, sorted by name. */
    suspend fun loadOptions(): LoadOptionsResult
    suspend fun create(purchase: NewPurchase): CreatePurchaseResult

    /** Every entry of [month]; the API has no summary endpoint, so callers aggregate these. */
    suspend fun listMonth(month: YearMonth): ListMonthResult
}

/**
 * A `401` here means the session could not be refreshed: `TokenRefresher` already ended it and `MainActivity`
 * goes to Login, so it is reported as [CreatePurchaseResult.Unexpected] / [LoadOptionsResult.Unexpected].
 */
class RemotePurchaseRepository(
    private val categoryApi: CategoryApi,
    private val paymentMethodApi: PaymentMethodApi,
    private val purchaseApi: PurchaseApi,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PurchaseRepository {
    private val byName = Collator.getInstance(Locale.forLanguageTag("pt-BR"))

    override suspend fun loadOptions(): LoadOptionsResult = withContext(ioDispatcher) {
        coroutineScope {
            val categories = async { json.apiCall { categoryApi.list() } }
            val paymentMethods = async { json.apiCall { paymentMethodApi.list() } }
            val categoriesOutcome = categories.await()
            val paymentMethodsOutcome = paymentMethods.await()

            if (categoriesOutcome is ApiOutcome.Success && paymentMethodsOutcome is ApiOutcome.Success) {
                LoadOptionsResult.Success(
                    categories = categoriesOutcome.value.sortedWith(compareBy(byName) { it.name }),
                    paymentMethods = paymentMethodsOutcome.value.sortedWith(compareBy(byName) { it.name }),
                )
            } else if (categoriesOutcome is ApiOutcome.NetworkError || paymentMethodsOutcome is ApiOutcome.NetworkError) {
                LoadOptionsResult.Network
            } else {
                LoadOptionsResult.Unexpected
            }
        }
    }

    override suspend fun create(purchase: NewPurchase): CreatePurchaseResult = withContext(ioDispatcher) {
        val request = PurchaseRequest(
            purchaseDate = purchase.date,
            description = purchase.description,
            categoryId = purchase.categoryId,
            paymentMethodId = purchase.paymentMethodId,
            amount = purchase.amount,
            status = purchase.status,
        )
        when (val outcome = json.apiCall { purchaseApi.create(request) }) {
            is ApiOutcome.Success -> CreatePurchaseResult.Success
            is ApiOutcome.HttpError -> when (outcome.status) {
                HTTP_NOT_FOUND -> CreatePurchaseResult.NotFound
                HTTP_UNPROCESSABLE_ENTITY -> invalidField(outcome.error?.message)
                    ?.let(CreatePurchaseResult::Invalid) ?: CreatePurchaseResult.Unexpected

                else -> CreatePurchaseResult.Unexpected
            }

            ApiOutcome.NetworkError -> CreatePurchaseResult.Network
            // The response body is not parsed (see PurchaseApi.create), so this can only be the request failing
            // to serialize, before anything was sent.
            ApiOutcome.MalformedResponse -> CreatePurchaseResult.Unexpected
        }
    }

    override suspend fun listMonth(month: YearMonth): ListMonthResult = withContext(ioDispatcher) {
        val items = mutableListOf<PurchaseDto>()
        var page = 1
        while (true) {
            val outcome = json.apiCall { purchaseApi.list(month.monthValue, month.year, page, PAGE_SIZE) }
            val result = when (outcome) {
                is ApiOutcome.Success -> outcome.value
                ApiOutcome.NetworkError -> return@withContext ListMonthResult.Network
                else -> return@withContext ListMonthResult.Unexpected
            }
            if (result.totalPages > MAX_PAGES) return@withContext ListMonthResult.Unexpected
            items += result.items
            if (page >= result.totalPages) break
            page++
        }
        ListMonthResult.Success(items)
    }

    // The API names the invalid field only at the start of the message ("amount must be ...").
    private fun invalidField(message: String?): PurchaseField? {
        val text = message.orEmpty()
        return apiFieldNames.entries.firstOrNull { text.startsWith(it.key) }?.value
    }

    companion object {
        /** API maximum (`purchase.MaxPageSize`). */
        const val PAGE_SIZE = 100

        /** Safety cap: 5,000 entries in one month (see spec 003, BR-2). */
        const val MAX_PAGES = 50

        private val apiFieldNames = mapOf(
            "purchase_date" to PurchaseField.DATE,
            "description" to PurchaseField.DESCRIPTION,
            "category_id" to PurchaseField.CATEGORY,
            "payment_method_id" to PurchaseField.PAYMENT_METHOD,
            "amount" to PurchaseField.AMOUNT,
        )
    }
}
