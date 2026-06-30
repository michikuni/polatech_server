package com.mpcorp.attendance.common.crypto

import kotlin.test.Test
import kotlin.test.assertEquals

class CryptoConstantsTest {

    @Test
    fun `algorithm constants match the EC P-256 ECDSA spec`() {
        assertEquals("EC", CryptoConstants.EC)
        assertEquals("SHA256withECDSA", CryptoConstants.SIGNATURE)
    }

    @Test
    fun `challenge size is 32 bytes`() {
        assertEquals(32, CryptoConstants.CHALLENGE_SIZE)
    }

    @Test
    fun `challenge expiry is 60 seconds`() {
        assertEquals(60, CryptoConstants.EXPIRE_SECONDS)
    }

    @Test
    fun `EC field size is 256 bits`() {
        assertEquals(256, CryptoConstants.EC_FIELD_SIZE_BITS)
    }
}
