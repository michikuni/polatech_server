package com.mpcorp.attendance.auth.service

import com.mpcorp.attendance.auth.dto.LoginRequest
import com.mpcorp.attendance.auth.dto.LoginResponse
import com.mpcorp.attendance.auth.repository.AdminRepository
import com.mpcorp.attendance.auth.security.JwtProperties
import com.mpcorp.attendance.auth.security.JwtService
import com.mpcorp.attendance.audit.entity.ActorType
import com.mpcorp.attendance.audit.entity.AuditAction
import com.mpcorp.attendance.audit.service.AuditService
import com.mpcorp.attendance.common.exception.InvalidCredentialsException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val adminRepository: AdminRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties,
    private val auditService: AuditService,
) {

    @Transactional(readOnly = true)
    fun login(request: LoginRequest): LoginResponse {
        val admin = adminRepository.findByUsername(request.username)
            ?: throw InvalidCredentialsException()

        if (!admin.enabled || !passwordEncoder.matches(request.password, admin.passwordHash)) {
            throw InvalidCredentialsException()
        }

        val token = jwtService.issue(admin.username)
        auditService.record(
            actorType = ActorType.ADMIN,
            action = AuditAction.ADMIN_LOGIN,
            actorId = admin.username,
            targetType = "ADMIN",
            targetId = admin.id?.toString(),
        )
        return LoginResponse(accessToken = token, expiresInSeconds = jwtProperties.expirationSeconds)
    }
}
