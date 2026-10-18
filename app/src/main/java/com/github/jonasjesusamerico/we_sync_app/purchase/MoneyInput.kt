package com.github.jonasjesusamerico.we_sync_app.purchase

import java.math.BigDecimal
import java.math.BigInteger
import java.text.NumberFormat
import java.util.Locale

/**
 * Cents-based money input: the typed digits fill the value from the right (`1234` → `R$ 12,34`), so the user
 * never types a decimal separator. Values are `BigDecimal` with scale 2.
 */
object MoneyInput {
    /** API column is `NUMERIC(10, 2)`. */
    val MAX_AMOUNT = BigDecimal("99999999.99")
    private const val MAX_DIGITS = 10

    private val brazil = Locale.forLanguageTag("pt-BR")

    /** Value of the digits in [text]; extra digits beyond the maximum are ignored. */
    fun parse(text: String): BigDecimal {
        val digits = text.filter(Char::isDigit).trimStart('0').take(MAX_DIGITS)
        val cents = if (digits.isEmpty()) BigInteger.ZERO else BigInteger(digits)
        return BigDecimal(cents, 2)
    }

    /** `R$ 1.234,56` in pt-BR. */
    fun format(amount: BigDecimal): String = NumberFormat.getCurrencyInstance(brazil).format(amount)
}
