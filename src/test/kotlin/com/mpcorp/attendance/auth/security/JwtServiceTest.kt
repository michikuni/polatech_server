package com.mpcorp.attendance.auth.security

import com.mpcorp.attendance.common.exception.UnauthorizedException
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JwtServiceTest {

    private val objectMapper: ObjectMapper = JsonMapper.builder().build()
    private val properties = JwtProperties(
        secret = "unit-test-secret-key-of-at-least-32-bytes-length!!",
        expirationSeconds = 3600,
        issuer = "attendance-test",
    )

    private fun serviceAt(epochSecond: Long): JwtService {
        val clock = Clock.fixed(Instant.ofEpochSecond(epochSecond), ZoneOffset.UTC)
        return JwtService(properties, clock, objectMapper)
    }

    @Test
    fun `issued token validates and returns the subject`() {
        val service = serviceAt(1_000_000)
        val token = service.issue("admin")
        assertEquals("admin", service.validateAndGetSubject(token))
    }

    @Test
    fun `token is rejected after it expires`() {
        val token = serviceAt(1_000_000).issue("admin") // exp = 1_003_600
        val afterExpiry = serviceAt(1_000_000 + 3600 + 1)
        assertFailsWith<UnauthorizedException> { afterExpiry.validateAndGetSubject(token) }
    }

    @Test
    fun `token with a tampered payload is rejected`() {
        val service = serviceAt(1_000_000)
        val parts = service.issue("admin").split(".")
        val forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
            """{"sub":"hacker","iss":"attendance-test","iat":1000000,"exp":2000000}""".toByteArray(),
        )
        val forged = "${parts[0]}.$forgedPayload.${parts[2]}"
        assertFailsWith<UnauthorizedException> { service.validateAndGetSubject(forged) }
    }

    @Test
    fun `token signed with a different secret is rejected`() {
        val token = serviceAt(1_000_000).issue("admin")
        val other = JwtService(
            properties.copy(secret = "a-totally-different-secret-key-32bytes-minimum!!"),
            Clock.fixed(Instant.ofEpochSecond(1_000_000), ZoneOffset.UTC),
            objectMapper,
        )
        assertFailsWith<UnauthorizedException> { other.validateAndGetSubject(token) }
    }

    @Test
    fun `malformed token is rejected`() {
        assertFailsWith<UnauthorizedException> { serviceAt(1_000_000).validateAndGetSubject("not-a-jwt") }
    }
}
