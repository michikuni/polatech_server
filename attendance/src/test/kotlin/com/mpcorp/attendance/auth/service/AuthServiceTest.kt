package com.mpcorp.attendance.auth.service

import com.mpcorp.attendance.auth.dto.LoginRequest
import com.mpcorp.attendance.auth.entity.Admin
import com.mpcorp.attendance.auth.repository.AdminRepository
import com.mpcorp.attendance.auth.security.JwtProperties
import com.mpcorp.attendance.auth.security.JwtService
import com.mpcorp.attendance.audit.service.AuditService
import com.mpcorp.attendance.common.exception.InvalidCredentialsException
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import tools.jackson.databind.json.JsonMapper
import java.time.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AuthServiceTest {

    private val adminRepository = mock(AdminRepository::class.java)
    private val passwordEncoder = BCryptPasswordEncoder()
    private val jwtProperties = JwtProperties(
        secret = "unit-test-secret-key-of-at-least-32-bytes-length!!",
        expirationSeconds = 3600,
        issuer = "attendance-test",
    )
    private val jwtService = JwtService(jwtProperties, Clock.systemUTC(), JsonMapper.builder().build())
    private val auditService = mock(AuditService::class.java)
    private val authService = AuthService(adminRepository, passwordEncoder, jwtService, jwtProperties, auditService)

    private fun admin(enabled: Boolean = true) = Admin(
        username = "admin",
        passwordHash = passwordEncoder.encode("secret")!!,
        displayName = "Admin",
        enabled = enabled,
    )

    @Test
    fun `login returns a valid token for correct credentials`() {
        given(adminRepository.findByUsername("admin")).willReturn(admin())

        val response = authService.login(LoginRequest("admin", "secret"))

        assertEquals("Bearer", response.tokenType)
        assertEquals(3600, response.expiresInSeconds)
        assertEquals("admin", jwtService.validateAndGetSubject(response.accessToken))
    }

    @Test
    fun `login rejects a wrong password`() {
        given(adminRepository.findByUsername("admin")).willReturn(admin())
        assertFailsWith<InvalidCredentialsException> { authService.login(LoginRequest("admin", "wrong")) }
    }

    @Test
    fun `login rejects an unknown user`() {
        given(adminRepository.findByUsername("ghost")).willReturn(null)
        assertFailsWith<InvalidCredentialsException> { authService.login(LoginRequest("ghost", "secret")) }
    }

    @Test
    fun `login rejects a disabled admin`() {
        given(adminRepository.findByUsername("admin")).willReturn(admin(enabled = false))
        assertFailsWith<InvalidCredentialsException> { authService.login(LoginRequest("admin", "secret")) }
    }
}
