package com.mpcorp.attendance.common.crypto

/**
 * Single source of truth for the cryptographic parameters of the attendance
 * challenge–response flow. Declared as an `object` because it is pure constants
 * (allowed by the "object only for Constants/Utility" rule).
 */
object CryptoConstants {

    /** Key algorithm of the elliptic-curve key pair. The device generates EC keys. */
    const val EC = "EC"

    /** ECDSA signature scheme: SHA-256 digest signed with the EC private key. */
    const val SIGNATURE = "SHA256withECDSA"

    /** Challenge length in bytes (256 bits of entropy). */
    const val CHALLENGE_SIZE = 32

    /** Time-to-live of a challenge, in seconds, after which it must be rejected. */
    const val EXPIRE_SECONDS = 60

    /** Expected EC field size in bits. P-256 (secp256r1) uses a 256-bit prime field. */
    const val EC_FIELD_SIZE_BITS = 256
}
