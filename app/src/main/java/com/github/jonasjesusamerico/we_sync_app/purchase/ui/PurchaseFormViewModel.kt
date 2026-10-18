package com.github.jonasjesusamerico.we_sync_app.purchase.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.jonasjesusamerico.we_sync_app.category.CategoryDto
import com.github.jonasjesusamerico.we_sync_app.paymentmethod.PaymentMethodDto
import com.github.jonasjesusamerico.we_sync_app.purchase.CreatePurchaseResult
import com.github.jonasjesusamerico.we_sync_app.purchase.LoadOptionsResult
import com.github.jonasjesusamerico.we_sync_app.purchase.PurchaseField
import com.github.jonasjesusamerico.we_sync_app.purchase.PurchaseFieldError
import com.github.jonasjesusamerico.we_sync_app.purchase.PurchaseForm
import com.github.jonasjesusamerico.we_sync_app.purchase.PurchaseRepository
import com.github.jonasjesusamerico.we_sync_app.purchase.PurchaseStatus
import com.github.jonasjesusamerico.we_sync_app.purchase.PurchaseValidation
import com.github.jonasjesusamerico.we_sync_app.purchase.PurchaseValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate

/** Message shown on the form (save failures) or in the options error block (load failures). */
enum class PurchaseFormMessage { NOT_FOUND, NETWORK, UNEXPECTED }

data class PurchaseFormUiState(
    val isLoadingOptions: Boolean = true,
    val optionsError: PurchaseFormMessage? = null,
    val categories: List<CategoryDto> = emptyList(),
    val paymentMethods: List<PaymentMethodDto> = emptyList(),
    val amount: BigDecimal = BigDecimal.ZERO.setScale(2),
    val date: LocalDate,
    val categoryId: Long? = null,
    val paymentMethodId: Long? = null,
    val status: PurchaseStatus = PurchaseStatus.PAID,
    val errors: Map<PurchaseField, PurchaseFieldError> = emptyMap(),
    val message: PurchaseFormMessage? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
) {
    val canEdit: Boolean get() = !isLoadingOptions && optionsError == null && !isSaving && !isSaved
}

/**
 * New entry form. The description is read from the field on submit (the EditText owns its text and restores
 * it); every other value lives here.
 */
class PurchaseFormViewModel(
    private val repository: PurchaseRepository,
    today: LocalDate = LocalDate.now(),
) : ViewModel() {
    private val _state = MutableStateFlow(PurchaseFormUiState(date = today))
    val state: StateFlow<PurchaseFormUiState> = _state.asStateFlow()

    init {
        loadOptions()
    }

    fun retryLoadOptions() {
        if (!_state.value.isLoadingOptions) loadOptions()
    }

    fun onAmountChanged(amount: BigDecimal) = _state.update {
        it.copy(amount = amount, errors = it.errors - PurchaseField.AMOUNT)
    }

    fun onDateSelected(date: LocalDate) = _state.update {
        it.copy(date = date, errors = it.errors - PurchaseField.DATE)
    }

    fun onCategorySelected(id: Long) = _state.update {
        it.copy(categoryId = id, errors = it.errors - PurchaseField.CATEGORY)
    }

    fun onPaymentMethodSelected(id: Long) = _state.update {
        it.copy(paymentMethodId = id, errors = it.errors - PurchaseField.PAYMENT_METHOD)
    }

    fun onStatusSelected(status: PurchaseStatus) = _state.update { it.copy(status = status) }

    fun onDescriptionChanged() = _state.update { it.copy(errors = it.errors - PurchaseField.DESCRIPTION) }

    fun submit(description: String) {
        val current = _state.value
        if (!current.canEdit) return

        val form = PurchaseForm(
            amount = current.amount,
            description = description,
            date = current.date,
            categoryId = current.categoryId,
            paymentMethodId = current.paymentMethodId,
            status = current.status,
        )
        when (val validation = PurchaseValidator.validate(form)) {
            is PurchaseValidation.Invalid -> _state.update { it.copy(errors = validation.errors, message = null) }
            is PurchaseValidation.Valid -> {
                _state.update { it.copy(errors = emptyMap(), message = null, isSaving = true) }
                viewModelScope.launch {
                    val result = repository.create(validation.purchase)
                    _state.update { it.copy(isSaving = false).withResult(result) }
                    if (result == CreatePurchaseResult.NotFound) loadOptions()
                }
            }
        }
    }

    private fun PurchaseFormUiState.withResult(result: CreatePurchaseResult) = when (result) {
        CreatePurchaseResult.Success -> copy(isSaved = true)
        CreatePurchaseResult.NotFound -> copy(message = PurchaseFormMessage.NOT_FOUND)
        is CreatePurchaseResult.Invalid -> copy(errors = mapOf(result.field to apiFieldError(result.field)))
        CreatePurchaseResult.Network -> copy(message = PurchaseFormMessage.NETWORK)
        CreatePurchaseResult.Unexpected -> copy(message = PurchaseFormMessage.UNEXPECTED)
    }

    // The client validator already covers these rules; a 422 here means the field is missing or out of range.
    private fun apiFieldError(field: PurchaseField) =
        if (field == PurchaseField.AMOUNT) PurchaseFieldError.AMOUNT_NOT_POSITIVE else PurchaseFieldError.REQUIRED

    private fun loadOptions() {
        _state.update { it.copy(isLoadingOptions = true, optionsError = null) }
        viewModelScope.launch {
            val result = repository.loadOptions()
            _state.update { state ->
                when (result) {
                    is LoadOptionsResult.Success -> state.copy(
                        isLoadingOptions = false,
                        categories = result.categories,
                        paymentMethods = result.paymentMethods,
                        // Keep a selection only if it still exists (it may have been deleted elsewhere).
                        categoryId = state.categoryId?.takeIf { id -> result.categories.any { it.id == id } },
                        paymentMethodId = state.paymentMethodId
                            ?.takeIf { id -> result.paymentMethods.any { it.id == id } },
                    )

                    LoadOptionsResult.Network ->
                        state.copy(isLoadingOptions = false, optionsError = PurchaseFormMessage.NETWORK)

                    LoadOptionsResult.Unexpected ->
                        state.copy(isLoadingOptions = false, optionsError = PurchaseFormMessage.UNEXPECTED)
                }
            }
        }
    }
}
