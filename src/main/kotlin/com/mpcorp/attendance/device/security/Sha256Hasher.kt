package com.mpcorp.attendance.device.security

import com.mpcorp.attendance.common.crypto.Base64Utils
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/** SHA-256 -> Base64 helper for pairing-code hashes and public-key fingerprints. */
@Component
class Sha256Hasher {

    fun hash(value: String): String = hash(value.toByteArray(StandardCharsets.UTF_8))

    fun hash(bytes: ByteArray): String =
        Base64Utils.encode(MessageDigest.getInstance("SHA-256").digest(bytes))
}
