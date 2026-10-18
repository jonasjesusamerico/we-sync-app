package com.github.jonasjesusamerico.we_sync_app.purchase

import com.github.jonasjesusamerico.we_sync_app.auth.apiError
import com.github.jonasjesusamerico.we_sync_app.auth.jsonResponse
import com.github.jonasjesusamerico.we_sync_app.auth.testJson
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.MediaType.Companion.toMediaType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class RemotePurchaseRepositoryTest {
    private val server = MockWebServer()
    private lateinit var repository: RemotePurchaseRepository

    private val purchase = NewPurchase(
        amount = BigDecimal("123.45"),
        description = "Mercado",
        date = LocalDate.of(2026, 10, 17),
        categoryId = 1,
        paymentMethodId = 2,
        status = PurchaseStatus.PAID,
    )

    @Before
    fun setUp() {
        server.start()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/api/v1/"))
            .addConverterFactory(testJson.asConverterFactory("application/json".toMediaType()))
            .build()
        repository = RemotePurchaseRepository(retrofit.create(), retrofit.create(), retrofit.create(), testJson)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun loadOptions_returnsBothListsSortedByName() = runTest {
        server.dispatcher = routes(
            categories = jsonResponse(200, """[
                {"id":2,"user_id":7,"name":"Viagem","is_global":false,"created_at":"x","updated_at":"x"},
                {"id":1,"user_id":null,"name":"Alimentação","is_global":true,"created_at":"x","updated_at":"x"}
            ]"""),
            paymentMethods = jsonResponse(200, """[{"id":3,"name":"Pix","is_global":true}]"""),
        )

        val result = repository.loadOptions() as LoadOptionsResult.Success

        assertEquals(listOf("Alimentação", "Viagem"), result.categories.map { it.name })
        assertEquals(listOf(3L), result.paymentMethods.map { it.id })
    }

    @Test
    fun loadOptions_failures() = runTest {
        server.dispatcher = routes(
            categories = jsonResponse(200, "[]"),
            paymentMethods = apiError(500, "INTERNAL_SERVER_ERROR", "internal server error"),
        )
        assertEquals(LoadOptionsResult.Unexpected, repository.loadOptions())

        server.close()
        assertEquals(LoadOptionsResult.Network, repository.loadOptions())
    }

    @Test
    fun create_sendsTheApiContract() = runTest {
        server.enqueue(jsonResponse(201, """{"id":10}"""))

        assertEquals(CreatePurchaseResult.Success, repository.create(purchase))
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/purchases", request.url.encodedPath)
        assertEquals(
            """{"purchase_date":"2026-10-17","description":"Mercado","category_id":1,"payment_method_id":2,""" +
                """"amount":"123.45","status":"PAID"}""",
            request.body?.utf8(),
        )
    }

    @Test
    fun create_successIgnoresTheResponseBody() = runTest {
        server.enqueue(jsonResponse(201, "not json"))

        assertEquals(CreatePurchaseResult.Success, repository.create(purchase))
    }

    @Test
    fun create_mapsErrors() = runTest {
        server.enqueue(apiError(404, "NOT_FOUND", "not found"))
        server.enqueue(apiError(422, "VALIDATION_ERROR", "amount must be greater than zero"))
        server.enqueue(apiError(422, "VALIDATION_ERROR", "payment_method_id must be a positive integer"))
        server.enqueue(apiError(422, "VALIDATION_ERROR", "status \"X\" is not valid"))
        server.enqueue(apiError(500, "INTERNAL_SERVER_ERROR", "internal server error"))
        server.enqueue(apiError(401, "UNAUTHORIZED", "invalid authentication token"))

        assertEquals(CreatePurchaseResult.NotFound, repository.create(purchase))
        assertEquals(CreatePurchaseResult.Invalid(PurchaseField.AMOUNT), repository.create(purchase))
        assertEquals(CreatePurchaseResult.Invalid(PurchaseField.PAYMENT_METHOD), repository.create(purchase))
        assertEquals(CreatePurchaseResult.Unexpected, repository.create(purchase))
        assertEquals(CreatePurchaseResult.Unexpected, repository.create(purchase))
        assertEquals(CreatePurchaseResult.Unexpected, repository.create(purchase))
    }

    @Test
    fun create_networkFailure() = runTest {
        server.close()

        assertEquals(CreatePurchaseResult.Network, repository.create(purchase))
    }

    @Test
    fun listMonth_requestsEveryPageWithTheMonthFilter() = runTest {
        server.enqueue(jsonResponse(200, """{"items":[${purchaseJson(1)},${purchaseJson(2)}],"total":3,"page":1,"page_size":100,"total_pages":2}"""))
        server.enqueue(jsonResponse(200, """{"items":[${purchaseJson(3, status = "CANCELED")}],"total":3,"page":2,"page_size":100,"total_pages":2}"""))

        val result = repository.listMonth(YearMonth.of(2026, 10)) as ListMonthResult.Success

        assertEquals(listOf(1L, 2L, 3L), result.items.map { it.id })
        assertEquals(PurchaseStatus.CANCELED, result.items.last().status)
        assertEquals(BigDecimal("10.00"), result.items.first().amount)
        val first = server.takeRequest().url
        assertEquals("/api/v1/purchases", first.encodedPath)
        assertEquals("10", first.queryParameter("month"))
        assertEquals("2026", first.queryParameter("year"))
        assertEquals("1", first.queryParameter("page"))
        assertEquals("100", first.queryParameter("page_size"))
        assertEquals("2", server.takeRequest().url.queryParameter("page"))
    }

    @Test
    fun listMonth_emptyMonthIsOneRequest() = runTest {
        server.enqueue(jsonResponse(200, """{"items":[],"total":0,"page":1,"page_size":100,"total_pages":0}"""))

        assertEquals(ListMonthResult.Success(emptyList()), repository.listMonth(YearMonth.of(2026, 10)))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun listMonth_failures() = runTest {
        server.enqueue(jsonResponse(200, """{"items":[],"total":5100,"page":1,"page_size":100,"total_pages":51}"""))
        assertEquals(ListMonthResult.Unexpected, repository.listMonth(YearMonth.of(2026, 10)))

        server.enqueue(jsonResponse(200, """{"items":[${purchaseJson(1)}],"total":2,"page":1,"page_size":100,"total_pages":2}"""))
        server.enqueue(apiError(500, "INTERNAL_SERVER_ERROR", "internal server error"))
        assertEquals(ListMonthResult.Unexpected, repository.listMonth(YearMonth.of(2026, 10)))

        server.close()
        assertEquals(ListMonthResult.Network, repository.listMonth(YearMonth.of(2026, 10)))
    }

    private fun routes(categories: MockResponse, paymentMethods: MockResponse) = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse = when (request.url.encodedPath) {
            "/api/v1/categories" -> categories
            "/api/v1/payment-methods" -> paymentMethods
            else -> MockResponse.Builder().code(404).build()
        }
    }
}
