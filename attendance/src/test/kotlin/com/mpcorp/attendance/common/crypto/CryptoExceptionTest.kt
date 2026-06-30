package com.mpcorp.attendance.common.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class CryptoExceptionTest {

    @Test
    fun `InvalidPublicKeyException carries its message and cause`() {
        val cause = IllegalArgumentException("bad input")
        val ex = InvalidPublicKeyException("invalid key", cause)

        assertEquals("invalid key", ex.message)
        assertSame(cause, ex.cause)
    }

    @Test
    fun `SignatureVerificationException carries its message`() {
        val ex = SignatureVerificationException("cannot verify")

        assertEquals("cannot verify", ex.message)
    }

    @Test
    fun `both subtypes belong to the RuntimeException-based CryptoException hierarchy`() {
        // These assignments compile only because the hierarchy holds, which is
        // exactly what the Global Exception Handler will rely on to catch
        // CryptoException as an unchecked RuntimeException.
        val asCrypto: CryptoException = InvalidPublicKeyException("x")
        val asRuntime: RuntimeException = SignatureVerificationException("y")

        assertEquals("x", asCrypto.message)
        assertEquals("y", asRuntime.message)
    }
}
