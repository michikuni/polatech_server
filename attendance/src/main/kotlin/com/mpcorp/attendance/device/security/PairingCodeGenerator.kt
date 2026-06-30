package com.mpcorp.attendance.device.security

import org.springframework.stereotype.Component
import java.security.SecureRandom

/**
 * Generates short, human-typable, one-time pairing codes using [SecureRandom].
 * The alphabet excludes ambiguous characters (0/O, 1/I) for readability.
 */
@Component
class PairingCodeGenerator(
    private val properties: EnrollmentProperties,
) {
    private val secureRandom = SecureRandom()

    fun generate(): String = buildString(properties.codeLength) {
        repeat(properties.codeLength) {
            append(ALPHABET[secureRandom.nextInt(ALPHABET.length)])
        }
    }

    private companion object {
        const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }
}
