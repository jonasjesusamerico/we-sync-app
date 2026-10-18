package com.github.jonasjesusamerico.we_sync_app.purchase.ui

import com.github.jonasjesusamerico.we_sync_app.auth.ui.MainDispatcherRule
import com.github.jonasjesusamerico.we_sync_app.category.CategoryDto
import com.github.jonasjesusamerico.we_sync_app.paymentmethod.PaymentMethodDto
import com.github.jonasjesusamerico.we_sync_app.purchase.CreatePurchaseResult
import com.github.jonasjesusamerico.we_sync_app.purchase.ListMonthResult
import com.github.jonasjesusamerico.we_sync_app.purchase.LoadOptionsResult
import com.github.jonasjesusamerico.we_sync_app.purchase.NewPurchase
import com.github.jonasjesusamerico.we_sync_app.purchase.PurchaseField
import com.github.jonasjesusamerico.we_sync_app.purchase.PurchaseFieldError
import com.github.jonasjesusamerico.we_sync_app.purchase.PurchaseRepository
import com.github.jonasjesusamerico.we_sync_app.purchase.PurchaseStatus
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

class PurchaseFormViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val today = LocalDate.of(2026, 10, 17)
    private val repository = FakePurchaseRepository()
    private val options = LoadOptionsResult.Success(
        categories = listOf(CategoryDto(1, "Alimentação", true), CategoryDto(2, "Viagem", false)),
        paymentMethods = listOf(PaymentMethodDto(3, "Pix", true)),
    )

    @Test
    fun defaults_todayAndPaid_whileLoadingOptions() {
        val viewModel = PurchaseFormViewModel(repository, today)

        val state = viewModel.state.value
        assertEquals(today, state.date)
        assertEquals(PurchaseStatus.PAID, state.status)
        assertTrue(state.isLoadingOptions)
        assertFalse(state.canEdit)
    }

    @Test
    fun optionsLoaded_enableTheForm() {
        val viewModel = loadedViewModel()

        assertEquals(options.categories, viewModel.state.value.categories)
        assertTrue(viewModel.state.value.canEdit)
    }

    @Test
    fun optionsFailure_showsErrorAndRetryReloads() {
        repository.optionsResults.add(CompletableDeferred(LoadOptionsResult.Network))
        val viewModel = PurchaseFormViewModel(repository, today)
        assertEquals(PurchaseFormMessage.NETWORK, viewModel.state.value.optionsError)

        repository.optionsResults.add(CompletableDeferred(options))
        viewModel.retryLoadOptions()

        assertNull(viewModel.state.value.optionsError)
        assertEquals(2, repository.loadCalls)
    }

    @Test
    fun invalidForm_showsErrorsWithoutRequest() {
        val viewModel = loadedViewModel()

        viewModel.submit(description = " ")

        assertEquals(
            mapOf(
                PurchaseField.AMOUNT to PurchaseFieldError.AMOUNT_NOT_POSITIVE,
                PurchaseField.DESCRIPTION to PurchaseFieldError.REQUIRED,
                PurchaseField.CATEGORY to PurchaseFieldError.REQUIRED,
                PurchaseField.PAYMENT_METHOD to PurchaseFieldError.REQUIRED,
            ),
            viewModel.state.value.errors,
        )
        assertEquals(0, repository.created.size)
    }

    @Test
    fun editingAField_clearsItsError() {
        val viewModel = loadedViewModel()
        viewModel.submit(description = "")

        viewModel.onAmountChanged(BigDecimal("1.00"))
        viewModel.onCategorySelected(1)

        assertFalse(PurchaseField.AMOUNT in viewModel.state.value.errors)
        assertFalse(PurchaseField.CATEGORY in viewModel.state.value.errors)
        assertTrue(PurchaseField.DESCRIPTION in viewModel.state.value.errors)
    }

    @Test
    fun validForm_sendsOnceAndFinishes() {
        val viewModel = filledViewModel()
        viewModel.onStatusSelected(PurchaseStatus.PENDING)

        viewModel.submit(description = " Mercado ")
        viewModel.submit(description = " Mercado ")
        assertTrue(viewModel.state.value.isSaving)
        assertFalse(viewModel.state.value.canEdit)

        repository.createResult.complete(CreatePurchaseResult.Success)

        assertEquals(
            listOf(NewPurchase(BigDecimal("12.34"), "Mercado", today, 1, 3, PurchaseStatus.PENDING)),
            repository.created,
        )
        assertTrue(viewModel.state.value.isSaved)
    }

    @Test
    fun notFound_showsMessageAndReloadsOptionsDroppingMissingSelection() {
        val viewModel = filledViewModel()
        viewModel.submit(description = "Mercado")
        repository.optionsResults.add(
            CompletableDeferred(options.copy(categories = listOf(CategoryDto(2, "Viagem", false)))),
        )

        repository.createResult.complete(CreatePurchaseResult.NotFound)

        val state = viewModel.state.value
        assertEquals(PurchaseFormMessage.NOT_FOUND, state.message)
        assertNull(state.categoryId)
        assertEquals(3L, state.paymentMethodId)
        assertEquals(BigDecimal("12.34"), state.amount)
    }

    @Test
    fun saveFailures_keepTheFormAndShowMessage() {
        val viewModel = filledViewModel()
        viewModel.submit(description = "Mercado")
        repository.createResult.complete(CreatePurchaseResult.Network)

        assertEquals(PurchaseFormMessage.NETWORK, viewModel.state.value.message)
        assertTrue(viewModel.state.value.canEdit)
        assertEquals(BigDecimal("12.34"), viewModel.state.value.amount)
    }

    @Test
    fun apiValidationError_mapsToTheField() {
        val viewModel = filledViewModel()
        viewModel.submit(description = "Mercado")
        repository.createResult.complete(CreatePurchaseResult.Invalid(PurchaseField.DATE))

        assertEquals(mapOf(PurchaseField.DATE to PurchaseFieldError.REQUIRED), viewModel.state.value.errors)
    }

    private fun loadedViewModel(): PurchaseFormViewModel {
        repository.optionsResults.add(CompletableDeferred(options))
        return PurchaseFormViewModel(repository, today)
    }

    private fun filledViewModel() = loadedViewModel().apply {
        onAmountChanged(BigDecimal("12.34"))
        onCategorySelected(1)
        onPaymentMethodSelected(3)
    }
}

/** Each loadOptions call takes the next deferred; create suspends until [createResult] completes. */
internal class FakePurchaseRepository : PurchaseRepository {
    val optionsResults = ArrayDeque<CompletableDeferred<LoadOptionsResult>>()
    var createResult = CompletableDeferred<CreatePurchaseResult>()
    val created = mutableListOf<NewPurchase>()
    var loadCalls = 0

    override suspend fun loadOptions(): LoadOptionsResult {
        loadCalls++
        return (optionsResults.removeFirstOrNull() ?: CompletableDeferred()).await()
    }

    override suspend fun create(purchase: NewPurchase): CreatePurchaseResult {
        created += purchase
        return createResult.await()
    }

    /** Each listMonth call records the month and takes the next deferred. */
    val monthResults = ArrayDeque<CompletableDeferred<ListMonthResult>>()
    val requestedMonths = mutableListOf<YearMonth>()

    override suspend fun listMonth(month: YearMonth): ListMonthResult {
        requestedMonths += month
        return (monthResults.removeFirstOrNull() ?: CompletableDeferred()).await()
    }
}
