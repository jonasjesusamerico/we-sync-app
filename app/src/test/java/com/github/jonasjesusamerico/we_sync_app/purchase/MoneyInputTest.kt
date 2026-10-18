package com.github.jonasjesusamerico.we_sync_app.purchase

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class MoneyInputTest {
    private val nbsp = ' ' // pt-BR currency format separates "R$" with a non-breaking space

    @Test
    fun digitsFillFromTheCents() {
        assertEquals(BigDecimal("0.00"), MoneyInput.parse(""))
        assertEquals(BigDecimal("0.01"), MoneyInput.parse("1"))
        assertEquals(BigDecimal("0.12"), MoneyInput.parse("12"))
        assertEquals(BigDecimal("1.23"), MoneyInput.parse("123"))
        assertEquals(BigDecimal("12.34"), MoneyInput.parse("R$ 12,34"))
    }

    @Test
    fun typingAndDeletingOnTheFormattedText() {
        assertEquals(BigDecimal("0.01"), MoneyInput.parse("R$${nbsp}0,001")) // typed "1" after R$ 0,00
        assertEquals(BigDecimal("1.23"), MoneyInput.parse("R$${nbsp}12,3")) // deleted the last digit of R$ 12,34
    }

    @Test
    fun extraDigitsBeyondTheMaximumAreIgnored() {
        assertEquals(MoneyInput.MAX_AMOUNT, MoneyInput.parse("9999999999"))
        assertEquals(BigDecimal("12345678.90"), MoneyInput.parse("12345678901"))
    }

    @Test
    fun formatsInBrazilianReal() {
        assertEquals("R$${nbsp}0,00", MoneyInput.format(BigDecimal("0.00")))
        assertEquals("R$${nbsp}1.234,56", MoneyInput.format(BigDecimal("1234.56")))
        assertEquals("R$${nbsp}99.999.999,99", MoneyInput.format(MoneyInput.MAX_AMOUNT))
    }
}
