package com.github.jonasjesusamerico.we_sync_app.auth

/** Field-level validation error shown inline on the form. */
enum class FieldError { REQUIRED, INVALID_EMAIL, PASSWORD_LENGTH, PASSWORD_MISMATCH }

data class CredentialErrors(
    val email: FieldError? = null,
    val password: FieldError? = null,
    val confirmation: FieldError? = null,
) {
    val isValid: Boolean get() = email == null && password == null && confirmation == null
}

/**
 * Client-side mirror of the API rules (`validateEmail` / `validatePassword` in
 * `../we-sync-api/internal/auth/service.go`). A convenience only: the API stays the authority.
 */
object CredentialsValidator {
    private val emailPattern = Regex("""^[^@\s]+@[^@\s]+\.[^@\s]+$""")
    private const val MAX_EMAIL_BYTES = 254
    private const val MIN_PASSWORD_BYTES = 8
    private const val MAX_PASSWORD_BYTES = 72

    fun validateLogin(email: String, password: String) = CredentialErrors(
        email = if (email.isBlank()) FieldError.REQUIRED else null,
        password = if (password.isEmpty()) FieldError.REQUIRED else null,
    )

    fun validateSignUp(email: String, password: String, confirmation: String) = CredentialErrors(
        email = emailError(email.trim()),
        password = passwordError(password),
        confirmation = when {
            confirmation.isEmpty() -> FieldError.REQUIRED
            confirmation != password -> FieldError.PASSWORD_MISMATCH
            else -> null
        },
    )

    private fun emailError(email: String): FieldError? = when {
        email.isEmpty() -> FieldError.REQUIRED
        email.utf8Size() > MAX_EMAIL_BYTES || !emailPattern.matches(email) -> FieldError.INVALID_EMAIL
        else -> null
    }

    // The API limit is in UTF-8 bytes (bcrypt), not characters.
    private fun passwordError(password: String): FieldError? = when {
        password.isEmpty() -> FieldError.REQUIRED
        password.utf8Size() !in MIN_PASSWORD_BYTES..MAX_PASSWORD_BYTES -> FieldError.PASSWORD_LENGTH
        else -> null
    }

    private fun String.utf8Size() = toByteArray(Charsets.UTF_8).size
}
