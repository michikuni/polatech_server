package com.mpcorp.attendance.auth.security

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * JWT signing/validation configuration, bound from `security.jwt.*`.
 * [secret] must be supplied via environment in production (>= 32 bytes for HS256).
 */
@ConfigurationProperties(prefix = "security.jwt")
data class JwtProperties(
    val secret: String,
    val expirationSeconds: Long,
    val issuer: String,
)
