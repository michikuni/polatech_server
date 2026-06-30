package com.mpcorp.attendance.common.crypto

import org.springframework.stereotype.Component
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.PublicKey
import java.security.Signature
import java.security.SignatureException

/**
 * Verifies an ECDSA (SHA256withECDSA) signature produced by a device over a
 * challenge, using the device's stored public key. Java Security API only.
 *
 * Concrete `@Component` for constructor injection into feature services.
 */
@Component
class SignatureVerifier {

    /**
     * @return `true` only if [signature] is a valid signature of [challenge] by
     *         the private key matching [publicKey]; `false` if it simply does
     *         not match (or the signature bytes are malformed).
     * @throws SignatureVerificationException if verification cannot be performed
     *         (e.g. the key is unusable or the algorithm is missing).
     */
    fun verify(publicKey: PublicKey, challenge: ByteArray, signature: ByteArray): Boolean {
        return try {
            val verifier = Signature.getInstance(CryptoConstants.SIGNATURE)
            verifier.initVerify(publicKey)
            verifier.update(challenge)
            verifier.verify(signature)
        } catch (e: SignatureException) {
            // Malformed or non-matching signature bytes → "not verified", not an error.
            false
        } catch (e: InvalidKeyException) {
            throw SignatureVerificationException("Public key cannot be used for ECDSA verification", e)
        } catch (e: NoSuchAlgorithmException) {
            throw SignatureVerificationException("Signature algorithm unavailable: ${CryptoConstants.SIGNATURE}", e)
        }
    }
}
