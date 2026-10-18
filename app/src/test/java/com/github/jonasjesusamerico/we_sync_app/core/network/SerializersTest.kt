package com.github.jonasjesusamerico.we_sync_app.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class SerializersTest {
    @Serializable
    private data class Sample(
        @Serializable(BigDecimalAsStringSerializer::class) val amount: BigDecimal,
        @Serializable(LocalDateSerializer::class) val date: LocalDate,
    )

    private val json = Json

    @Test
    fun encodesAmountAsDecimalStringAndDateAsIso() {
        val encoded = json.encodeToString(Sample.serializer(), Sample(BigDecimal("1234.50"), LocalDate.of(2026, 1, 5)))

        assertEquals("""{"amount":"1234.50","date":"2026-01-05"}""", encoded)
    }

    @Test
    fun decodesAmountFromStringOrNumber() {
        assertEquals(BigDecimal("12.34"), json.decodeFromString(Sample.serializer(), """{"amount":"12.34","date":"2026-01-05"}""").amount)
        assertEquals(BigDecimal("12.34"), json.decodeFromString(Sample.serializer(), """{"amount":12.34,"date":"2026-01-05"}""").amount)
    }

    @Test(expected = SerializationException::class)
    fun rejectsInvalidAmount() {
        json.decodeFromString(Sample.serializer(), """{"amount":"abc","date":"2026-01-05"}""")
    }

    @Test(expected = SerializationException::class)
    fun rejectsInvalidDate() {
        json.decodeFromString(Sample.serializer(), """{"amount":"1.00","date":"05/01/2026"}""")
    }
}
