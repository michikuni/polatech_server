package com.mpcorp.attendance.common.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class Base64UtilsTest {

    @Test
    fun `encode then decode returns the original bytes`() {
        val original = byteArrayOf(1, 2, 3, 4, 5, -1, -128, 127)
        val roundTrip = Base64Utils.decode(Base64Utils.encode(original))
        assertContentEquals(original, roundTrip)
    }

    @Test
    fun `encode produces standard Base64`() {
        val bytes = "Hello".toByteArray(Charsets.US_ASCII)
        assertEquals("SGVsbG8=", Base64Utils.encode(bytes))
    }

    @Test
    fun `empty input round-trips to an empty array`() {
        assertEquals("", Base64Utils.encode(ByteArray(0)))
        assertContentEquals(ByteArray(0), Base64Utils.decode(""))
    }

    @Test
    fun `decode rejects characters outside the Base64 alphabet`() {
        assertFailsWith<IllegalArgumentException> {
            Base64Utils.decode("!!not-base64!!")
        }
    }
}
