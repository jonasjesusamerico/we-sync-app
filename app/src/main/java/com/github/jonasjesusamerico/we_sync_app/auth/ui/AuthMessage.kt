package com.github.jonasjesusamerico.we_sync_app.auth.ui

import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import com.google.android.material.color.MaterialColors
import com.github.jonasjesusamerico.we_sync_app.R
import com.github.jonasjesusamerico.we_sync_app.auth.FieldError

/** Screen-level message shown below the form; [isError] selects the error or the informative color. */
enum class AuthMessage(@StringRes val textRes: Int, val isError: Boolean = true) {
    INVALID_CREDENTIALS(R.string.error_invalid_credentials),
    EMAIL_TAKEN(R.string.error_email_taken),
    ACCOUNT_CREATED(R.string.message_account_created, isError = false),
    NETWORK(R.string.error_network),
    UNEXPECTED(R.string.error_unexpected),
}

/** Shows [message] in this view, or hides it when null. */
fun TextView.showAuthMessage(message: AuthMessage?) {
    isVisible = message != null
    message ?: return
    setText(message.textRes)
    val colorAttr = if (message.isError) androidx.appcompat.R.attr.colorError else androidx.appcompat.R.attr.colorPrimary
    setTextColor(MaterialColors.getColor(this, colorAttr))
}

@StringRes
fun FieldError.textRes(): Int = when (this) {
    FieldError.REQUIRED -> R.string.error_field_required
    FieldError.INVALID_EMAIL -> R.string.error_invalid_email
    FieldError.PASSWORD_LENGTH -> R.string.error_password_length
    FieldError.PASSWORD_MISMATCH -> R.string.error_password_mismatch
}
