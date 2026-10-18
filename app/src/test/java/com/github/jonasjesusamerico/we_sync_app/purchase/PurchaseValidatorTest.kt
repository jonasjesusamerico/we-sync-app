package com.github.jonasjesusamerico.we_sync_app.purchase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class PurchaseValidatorTest {
    private val valid = PurchaseForm(
        amount = BigDecimal("12.34"),
        description = "  Mercado  ",
        date = LocalDate.of(2026, 10, 17),
        categoryId = 1,
        paymentMethodId = 2,
        status = PurchaseStatus.PAID,
    )

    @Test
    fun validForm_isTrimmedAndScaled() {
        val result = PurchaseValidator.validate(valid.copy(amount = BigDecimal("12.3")))

        val purchase = (result as PurchaseValidation.Valid).purchase
        assertEquals("Mercado", purchase.description)
        assertEquals(BigDecimal("12.30"), purchase.amount)
    }

    @Test
    fun amountLimits() {
        assertEquals(PurchaseFieldError.AMOUNT_NOT_POSITIVE, errorsOf(valid.copy(amount = BigDecimal("0.00")))[PurchaseField.AMOUNT])
        assertTrue(PurchaseValidator.validate(valid.copy(amount = BigDecimal("0.01"))) is PurchaseValidation.Valid)
        assertTrue(PurchaseValidator.validate(valid.copy(amount = MoneyInput.MAX_AMOUNT)) is PurchaseValidation.Valid)
        assertEquals(PurchaseFieldError.AMOUNT_TOO_HIGH, errorsOf(valid.copy(amount = BigDecimal("100000000.00")))[PurchaseField.AMOUNT])
        assertEquals(PurchaseFieldError.AMOUNT_INVALID, errorsOf(valid.copy(amount = BigDecimal("1.234")))[PurchaseField.AMOUNT])
    }

    @Test
    fun descriptionRequiredAndLimited() {
        assertEquals(PurchaseFieldError.REQUIRED, errorsOf(valid.copy(description = "   "))[PurchaseField.DESCRIPTION])
        assertTrue(PurchaseValidator.validate(valid.copy(description = "a".repeat(255))) is PurchaseValidation.Valid)
        assertEquals(PurchaseFieldError.TOO_LONG, errorsOf(valid.copy(description = "a".repeat(256)))[PurchaseField.DESCRIPTION])
    }

    @Test
    fun selectionsAndDateRequired() {
        val errors = errorsOf(valid.copy(date = null, categoryId = null, paymentMethodId = null))

        assertEquals(
            mapOf(
                PurchaseField.DATE to PurchaseFieldError.REQUIRED,
                PurchaseField.CATEGORY to PurchaseFieldError.REQUIRED,
                PurchaseField.PAYMENT_METHOD to PurchaseFieldError.REQUIRED,
            ),
            errors,
        )
    }

    private fun errorsOf(form: PurchaseForm) = (PurchaseValidator.validate(form) as PurchaseValidation.Invalid).errors
}
