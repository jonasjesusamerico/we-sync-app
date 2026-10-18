package com.github.jonasjesusamerico.we_sync_app.purchase

import java.math.BigDecimal
import java.time.LocalDate

enum class PurchaseField { AMOUNT, DESCRIPTION, DATE, CATEGORY, PAYMENT_METHOD }

enum class PurchaseFieldError { REQUIRED, AMOUNT_NOT_POSITIVE, AMOUNT_TOO_HIGH, AMOUNT_INVALID, TOO_LONG }

/** Form values as typed; selections are null until chosen. */
data class PurchaseForm(
    val amount: BigDecimal,
    val description: String,
    val date: LocalDate?,
    val categoryId: Long?,
    val paymentMethodId: Long?,
    val status: PurchaseStatus,
)

/** A validated entry, ready to send. */
data class NewPurchase(
    val amount: BigDecimal,
    val description: String,
    val date: LocalDate,
    val categoryId: Long,
    val paymentMethodId: Long,
    val status: PurchaseStatus,
)

sealed interface PurchaseValidation {
    data class Valid(val purchase: NewPurchase) : PurchaseValidation
    data class Invalid(val errors: Map<PurchaseField, PurchaseFieldError>) : PurchaseValidation
}

/**
 * Client-side mirror of `validateRequest` in `../we-sync-api/internal/purchase/service.go` plus the column
 * limits of migration `000005`. A convenience only: the API stays the authority.
 */
object PurchaseValidator {
    const val MAX_DESCRIPTION_LENGTH = 255

    fun validate(form: PurchaseForm): PurchaseValidation {
        val description = form.description.trim()
        val errors = buildMap {
            amountError(form.amount)?.let { put(PurchaseField.AMOUNT, it) }
            when {
                description.isEmpty() -> put(PurchaseField.DESCRIPTION, PurchaseFieldError.REQUIRED)
                description.length > MAX_DESCRIPTION_LENGTH -> put(PurchaseField.DESCRIPTION, PurchaseFieldError.TOO_LONG)
            }
            if (form.date == null) put(PurchaseField.DATE, PurchaseFieldError.REQUIRED)
            if (form.categoryId == null) put(PurchaseField.CATEGORY, PurchaseFieldError.REQUIRED)
            if (form.paymentMethodId == null) put(PurchaseField.PAYMENT_METHOD, PurchaseFieldError.REQUIRED)
        }
        if (errors.isNotEmpty()) return PurchaseValidation.Invalid(errors)

        return PurchaseValidation.Valid(
            NewPurchase(
                amount = form.amount.setScale(2),
                description = description,
                date = requireNotNull(form.date),
                categoryId = requireNotNull(form.categoryId),
                paymentMethodId = requireNotNull(form.paymentMethodId),
                status = form.status,
            ),
        )
    }

    private fun amountError(amount: BigDecimal): PurchaseFieldError? = when {
        amount.signum() <= 0 -> PurchaseFieldError.AMOUNT_NOT_POSITIVE
        amount.stripTrailingZeros().scale() > 2 -> PurchaseFieldError.AMOUNT_INVALID
        amount > MoneyInput.MAX_AMOUNT -> PurchaseFieldError.AMOUNT_TOO_HIGH
        else -> null
    }
}
