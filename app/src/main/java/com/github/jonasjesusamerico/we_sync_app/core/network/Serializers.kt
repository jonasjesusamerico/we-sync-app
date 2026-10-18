package com.github.jonasjesusamerico.we_sync_app.core.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Money as a decimal string (`"12.34"`), the format the API uses; never `Double`.
 * Also accepts an unquoted JSON number when reading.
 */
object BigDecimalAsStringSerializer : KSerializer<BigDecimal> {
    override val descriptor = PrimitiveSerialDescriptor("BigDecimalAsString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigDecimal) = encoder.encodeString(value.toPlainString())

    override fun deserialize(decoder: Decoder): BigDecimal {
        val raw = (decoder as? JsonDecoder)?.decodeJsonElement()?.jsonPrimitive?.content ?: decoder.decodeString()
        return try {
            BigDecimal(raw)
        } catch (e: NumberFormatException) {
            throw SerializationException("invalid decimal: $raw", e)
        }
    }
}

/** Date without time as `YYYY-MM-DD` (API `purchase.Date`). */
object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): LocalDate {
        val raw = decoder.decodeString()
        return try {
            LocalDate.parse(raw)
        } catch (e: DateTimeParseException) {
            throw SerializationException("invalid date: $raw", e)
        }
    }
}
