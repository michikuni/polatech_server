package com.mpcorp.attendance.common.crypto

/**
 * Base type for every error raised by the crypto building blocks.
 *
 * Sealed so the Global Exception Handler (added with the web/feature layer) can
 * map subtypes exhaustively to HTTP responses.
 */
sealed class CryptoException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/** The supplied Base64 string is not a valid EC (P-256) public key. */
class InvalidPublicKeyException(
    message: String,
    cause: Throwable? = null,
) : CryptoException(message, cause)

/**
 * Verification could not be performed because of a configuration/key error.
 * Note: a plain "signature does not match" is NOT an error — that returns `false`.
 */
class SignatureVerificationException(
    message: String,
    cause: Throwable? = null,
) : CryptoException(message, cause)
