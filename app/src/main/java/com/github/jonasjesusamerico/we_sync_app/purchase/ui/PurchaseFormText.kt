package com.github.jonasjesusamerico.we_sync_app.purchase.ui

import androidx.annotation.StringRes
import com.github.jonasjesusamerico.we_sync_app.R
import com.github.jonasjesusamerico.we_sync_app.purchase.PurchaseFieldError

@StringRes
fun PurchaseFieldError.textRes(): Int = when (this) {
    PurchaseFieldError.REQUIRED -> R.string.error_field_required
    PurchaseFieldError.AMOUNT_NOT_POSITIVE -> R.string.error_amount_not_positive
    PurchaseFieldError.AMOUNT_TOO_HIGH -> R.string.error_amount_too_high
    PurchaseFieldError.AMOUNT_INVALID -> R.string.error_amount_invalid
    PurchaseFieldError.TOO_LONG -> R.string.error_description_too_long
}

@StringRes
fun PurchaseFormMessage.textRes(): Int = when (this) {
    PurchaseFormMessage.NOT_FOUND -> R.string.error_purchase_reference_not_found
    PurchaseFormMessage.NETWORK -> R.string.error_network
    PurchaseFormMessage.UNEXPECTED -> R.string.error_unexpected
}
