package com.mpcorp.attendance.auth.bootstrap

import com.mpcorp.attendance.auth.entity.Admin
import com.mpcorp.attendance.auth.repository.AdminRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Creates the first admin from configuration at startup if none exists. No
 * credential is hardcoded: if no password is configured, creation is skipped
 * with a warning.
 */
@Component
class AdminBootstrap(
    private val adminRepository: AdminRepository,
    private val passwordEncoder: PasswordEncoder,
    private val properties: AdminBootstrapProperties,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (adminRepository.count() > 0) return

        val password: String = properties.password?.takeIf { it.isNotBlank() }
            ?: run {
                log.warn(
                    "No admin exists and app.bootstrap.admin.password is not set. " +
                        "Set ADMIN_PASSWORD to create the first admin.",
                )
                return
            }

        val passwordHash = passwordEncoder.encode(password)
            ?: error("Password encoder returned a null hash")

        val admin = Admin(
            username = properties.username,
            passwordHash = passwordHash,
            displayName = "Administrator",
        )
        adminRepository.save(admin)
        log.info("Bootstrapped initial admin '{}'", properties.username)
    }
}
