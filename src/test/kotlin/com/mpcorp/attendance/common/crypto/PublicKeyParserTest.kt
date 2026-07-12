package com.mpcorp.attendance.common.crypto

import java.security.interfaces.ECPublicKey
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PublicKeyParserTest {

    private val parser = PublicKeyParser()

    @Test
    fun `parses a valid P-256 public key`() {
        val keyPair = CryptoTestSupport.generateEcKeyPair()
        val base64 = Base64Utils.encode(keyPair.public.encoded)

        val parsed = parser.parse(base64)

        assertTrue(parsed is ECPublicKey)
        assertEquals("EC", parsed.algorithm)
        assertContentEquals(keyPair.public.encoded, parsed.encoded)
    }

    @Test
    fun `rejects an invalid Base64 string`() {
        assertFailsWith<InvalidPublicKeyException> {
            parser.parse("!!not-base64!!")
        }
    }

    @Test
    fun `rejects well-formed Base64 that is not an X509 key`() {
        val notAKey = Base64Utils.encode(byteArrayOf(1, 2, 3, 4))
        assertFailsWith<InvalidPublicKeyException> {
            parser.parse(notAKey)
        }
    }

    @Test
    fun `rejects a non P-256 EC key`() {
        val p384 = CryptoTestSupport.generateEcKeyPair(CryptoTestSupport.P384)
        val base64 = Base64Utils.encode(p384.public.encoded)

        assertFailsWith<InvalidPublicKeyException> {
            parser.parse(base64)
        }
    }

    @Test
    fun `rejects an RSA key`() {
        val rsa = CryptoTestSupport.generateRsaKeyPair()
        val base64 = Base64Utils.encode(rsa.public.encoded)

        assertFailsWith<InvalidPublicKeyException> {
            parser.parse(base64)
        }
    }
}
