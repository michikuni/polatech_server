package com.mpcorp.attendance.auth.security

import com.mpcorp.attendance.common.exception.UnauthorizedException
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Minimal HS256 JWT implementation using only the JDK ([javax.crypto.Mac]) plus
 * Jackson for the JSON segments — no external JWT/crypto library. Tokens are
 * `base64url(header).base64url(payload).base64url(HMAC-SHA256(header.payload))`.
 */
@Service
class JwtService(
    private val properties: JwtProperties,
    private val clock: Clock,
    private val objectMapper: ObjectMapper,
) {
    private val urlEncoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()
    private val urlDecoder: Base64.Decoder = Base64.getUrlDecoder()

    fun issue(subject: String): String {
        val now = Instant.now(clock)
        val header = linkedMapOf("alg" to "HS256", "typ" to "JWT")
        val payload = linkedMapOf(
            "sub" to subject,
            "iss" to properties.issuer,
            "iat" to now.epochSecond,
            "exp" to now.plusSeconds(properties.expirationSeconds).epochSecond,
        )
        val unsigned = "${encodeSegment(header)}.${encodeSegment(payload)}"
        return "$unsigned.${sign(unsigned)}"
    }

    fun validateAndGetSubject(token: String): String {
        val parts = token.split(".")
        if (parts.size != 3) throw UnauthorizedException("Malformed token")

        val unsigned = "${parts[0]}.${parts[1]}"
        val expected = sign(unsigned)
        val matches = MessageDigest.isEqual(
            expected.toByteArray(StandardCharsets.US_ASCII),
            parts[2].toByteArray(StandardCharsets.US_ASCII),
        )
        if (!matches) throw UnauthorizedException("Invalid token signature")

        @Suppress("UNCHECKED_CAST")
        val claims = objectMapper.readValue(urlDecoder.decode(parts[1]), Map::class.java) as Map<String, Any?>
        val exp = (claims["exp"] as? Number)?.toLong() ?: throw UnauthorizedException("Missing token expiry")
        if (Instant.now(clock).epochSecond >= exp) throw UnauthorizedException("Token expired")
        return claims["sub"] as? String ?: throw UnauthorizedException("Missing token subject")
    }

    private fun encodeSegment(value: Any): String =
        urlEncoder.encodeToString(objectMapper.writeValueAsBytes(value))

    private fun sign(data: String): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(properties.secret.toByteArray(StandardCharsets.UTF_8), HMAC_ALGORITHM))
        return urlEncoder.encodeToString(mac.doFinal(data.toByteArray(StandardCharsets.US_ASCII)))
    }

    private companion object {
        const val HMAC_ALGORITHM = "HmacSHA256"
    }
}
