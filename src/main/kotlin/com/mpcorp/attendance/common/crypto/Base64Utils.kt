package com.mpcorp.attendance.common.crypto

import java.util.Base64

/**
 * Pure, stateless Base64 helper for converting between binary crypto material
 * (public keys, signatures, challenges) and their textual transport form.
 *
 * Declared as an `object`: no state, no business logic — exactly the "Utility
 * thuần" case where an object is permitted.
 */
object Base64Utils {

    private val encoder: Base64.Encoder = Base64.getEncoder()
    private val decoder: Base64.Decoder = Base64.getDecoder()

    /** Encodes raw bytes to a standard Base64 string. */
    fun encode(data: ByteArray): String = encoder.encodeToString(data)

    /**
     * Decodes a standard Base64 string back to bytes.
     * @throws IllegalArgumentException if [value] is not valid Base64.
     */
    fun decode(value: String): ByteArray = decoder.decode(value)
}
