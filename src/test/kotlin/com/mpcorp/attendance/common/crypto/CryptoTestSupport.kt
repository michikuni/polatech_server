package com.mpcorp.attendance.common.crypto

import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec

/**
 * Test-only helper that plays the role of the DEVICE: it generates EC key pairs
 * and signs data with the private key. Uses only the Java Security API, exactly
 * like a real client would. Never used by production code.
 */
object CryptoTestSupport {

    const val P256 = "secp256r1"
    const val P384 = "secp384r1"

    fun generateEcKeyPair(curve: String = P256): KeyPair {
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec(curve))
        return generator.generateKeyPair()
    }

    fun generateRsaKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        return generator.generateKeyPair()
    }

    fun sign(data: ByteArray, privateKey: PrivateKey): ByteArray {
        val signature = Signature.getInstance(CryptoConstants.SIGNATURE)
        signature.initSign(privateKey)
        signature.update(data)
        return signature.sign()
    }
}
