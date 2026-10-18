package com.github.jonasjesusamerico.we_sync_app.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialsValidatorTest {

    @Test
    fun login_requiresBothFields() {
        val errors = CredentialsValidator.validateLogin(email = "  ", password = "")
        assertEquals(FieldError.REQUIRED, errors.email)
        assertEquals(FieldError.REQUIRED, errors.password)
    }

    @Test
    fun login_doesNotApplySignUpRules() {
        assertTrue(CredentialsValidator.validateLogin(email = "not-an-email", password = "1").isValid)
    }

    @Test
    fun signUp_acceptsValidInput() {
        assertTrue(signUp(email = "  user@example.com ", password = "12345678").isValid)
    }

    @Test
    fun signUp_rejectsInvalidEmails() {
        listOf("user", "user@example", "us er@example.com", "a@b@c.com").forEach {
            assertEquals(it, FieldError.INVALID_EMAIL, signUp(email = it).email)
        }
    }

    @Test
    fun signUp_emailLimitIs254Bytes() {
        val domain = "@example.com"
        assertEquals(null, signUp(email = "a".repeat(254 - domain.length) + domain).email)
        assertEquals(FieldError.INVALID_EMAIL, signUp(email = "a".repeat(255 - domain.length) + domain).email)
    }

    @Test
    fun signUp_passwordLimitsAreInUtf8Bytes() {
        assertEquals(FieldError.PASSWORD_LENGTH, signUp(password = "1234567").password)
        assertEquals(null, signUp(password = "12345678").password)
        assertEquals(null, signUp(password = "a".repeat(72)).password)
        assertEquals(FieldError.PASSWORD_LENGTH, signUp(password = "a".repeat(73)).password)
        // "é" is 2 bytes: 37 characters = 74 bytes.
        assertEquals(FieldError.PASSWORD_LENGTH, signUp(password = "é".repeat(37)).password)
        // 4 characters = 8 bytes.
        assertEquals(null, signUp(password = "éééé").password)
    }

    @Test
    fun signUp_requiresMatchingConfirmation() {
        assertEquals(
            FieldError.PASSWORD_MISMATCH,
            CredentialsValidator.validateSignUp("user@example.com", "12345678", "12345679").confirmation,
        )
        assertEquals(
            FieldError.REQUIRED,
            CredentialsValidator.validateSignUp("user@example.com", "12345678", "").confirmation,
        )
    }

    private fun signUp(email: String = "user@example.com", password: String = "12345678") =
        CredentialsValidator.validateSignUp(email, password, password)
}
