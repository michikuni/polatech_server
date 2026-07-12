package com.mpcorp.attendance.common.crypto

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SignatureVerifierTest {

    private val verifier = SignatureVerifier()
    private val challengeGenerator: ChallengeGenerator = SecureRandomChallengeGenerator()

    @Test
    fun `verifies a genuine signature over the challenge`() {
        val keyPair = CryptoTestSupport.generateEcKeyPair()
        val challenge = challengeGenerator.generate()
        val signature = CryptoTestSupport.sign(challenge, keyPair.private)

        assertTrue(verifier.verify(keyPair.public, challenge, signature))
    }

    @Test
    fun `rejects a signature when the challenge was tampered with`() {
        val keyPair = CryptoTestSupport.generateEcKeyPair()
        val challenge = challengeGenerator.generate()
        val signature = CryptoTestSupport.sign(challenge, keyPair.private)

        val tampered = challenge.copyOf().also { it[0] = (it[0] + 1).toByte() }

        assertFalse(verifier.verify(keyPair.public, tampered, signature))
    }

    @Test
    fun `rejects a signature made by a different key`() {
        val signer = CryptoTestSupport.generateEcKeyPair()
        val other = CryptoTestSupport.generateEcKeyPair()
        val challenge = challengeGenerator.generate()
        val signature = CryptoTestSupport.sign(challenge, signer.private)

        assertFalse(verifier.verify(other.public, challenge, signature))
    }

    @Test
    fun `rejects malformed signature bytes without throwing`() {
        val keyPair = CryptoTestSupport.generateEcKeyPair()
        val challenge = challengeGenerator.generate()
        val garbage = byteArrayOf(0, 1, 2, 3, 4, 5)

        assertFalse(verifier.verify(keyPair.public, challenge, garbage))
    }

    @Test
    fun `throws when the key cannot be used for ECDSA`() {
        val rsa = CryptoTestSupport.generateRsaKeyPair()
        val challenge = challengeGenerator.generate()

        assertFailsWith<SignatureVerificationException> {
            verifier.verify(rsa.public, challenge, byteArrayOf(0, 1, 2, 3))
        }
    }
}
