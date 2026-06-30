package com.mpcorp.attendance.common.crypto

import org.springframework.stereotype.Component
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.PublicKey
import java.security.interfaces.ECPublicKey
import java.security.spec.X509EncodedKeySpec

/**
 * Parses a Base64-encoded X.509 (SubjectPublicKeyInfo) EC public key into a
 * [java.security.PublicKey].
 *
 * The backend only ever holds the PUBLIC key; the private key never leaves the
 * device. Uses only the Java Security API — no external crypto library.
 *
 * A concrete `@Component` (not an interface): there is a single correct way to
 * decode an X.509 EC key, so it is injected directly via constructor injection.
 */
@Component
class PublicKeyParser {

    fun parse(base64Key: String): PublicKey {
        val der = try {
            Base64Utils.decode(base64Key)
        } catch (e: IllegalArgumentException) {
            throw InvalidPublicKeyException("Public key is not valid Base64", e)
        }

        val publicKey = try {
            val keySpec = X509EncodedKeySpec(der)
            KeyFactory.getInstance(CryptoConstants.EC).generatePublic(keySpec)
        } catch (e: GeneralSecurityException) {
            throw InvalidPublicKeyException("Public key is not a valid EC key", e)
        }

        requireP256(publicKey)
        return publicKey
    }

    private fun requireP256(publicKey: PublicKey) {
        val ecKey = publicKey as? ECPublicKey
            ?: throw InvalidPublicKeyException("Public key is not an EC key: ${publicKey.algorithm}")
        val fieldSize = ecKey.params.curve.field.fieldSize
        if (fieldSize != CryptoConstants.EC_FIELD_SIZE_BITS) {
            throw InvalidPublicKeyException(
                "Expected a P-256 key (${CryptoConstants.EC_FIELD_SIZE_BITS}-bit field) but got $fieldSize-bit",
            )
        }
    }
}
