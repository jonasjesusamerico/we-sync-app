package com.github.jonasjesusamerico.we_sync_app.auth.ui

import androidx.annotation.StringRes
import com.github.jonasjesusamerico.we_sync_app.R
import com.github.jonasjesusamerico.we_sync_app.auth.FieldError

/** Screen-level message shown below the form. */
enum class AuthMessage(@StringRes val textRes: Int) {
    INVALID_CREDENTIALS(R.string.error_invalid_credentials),
    EMAIL_TAKEN(R.string.error_email_taken),
    ACCOUNT_CREATED(R.string.message_account_created),
    NETWORK(R.string.error_network),
    UNEXPECTED(R.string.error_unexpected),
}

@StringRes
fun FieldError.textRes(): Int = when (this) {
    FieldError.REQUIRED -> R.string.error_field_required
    FieldError.INVALID_EMAIL -> R.string.error_invalid_email
    FieldError.PASSWORD_LENGTH -> R.string.error_password_length
    FieldError.PASSWORD_MISMATCH -> R.string.error_password_mismatch
}
