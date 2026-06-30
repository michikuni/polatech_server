package com.mpcorp.attendance.audit.service

import com.mpcorp.attendance.audit.entity.ActorType
import com.mpcorp.attendance.audit.entity.AuditAction
import com.mpcorp.attendance.audit.entity.AuditLog
import com.mpcorp.attendance.audit.repository.AuditLogRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

/**
 * Writes append-only audit records. Each write runs in its OWN transaction
 * ([Propagation.REQUIRES_NEW]) so that security events (e.g. a failed signature)
 * are persisted even when the caller's transaction rolls back, and a failed
 * audit insert never breaks the caller.
 */
@Service
class AuditService(
    private val auditLogRepository: AuditLogRepository,
    private val clock: Clock,
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun record(
        actorType: ActorType,
        action: AuditAction,
        actorId: String? = null,
        targetType: String? = null,
        targetId: String? = null,
        detail: String? = null,
        ip: String? = null,
    ) {
        persist(actorType, action, actorId, targetType, targetId, detail, ip)
    }

    /** Records an ADMIN action, resolving the actor from the current security context. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun recordAdminAction(
        action: AuditAction,
        targetType: String? = null,
        targetId: String? = null,
        detail: String? = null,
    ) {
        val username = SecurityContextHolder.getContext().authentication?.name
        persist(ActorType.ADMIN, action, username, targetType, targetId, detail, null)
    }

    private fun persist(
        actorType: ActorType,
        action: AuditAction,
        actorId: String?,
        targetType: String?,
        targetId: String?,
        detail: String?,
        ip: String?,
    ) {
        auditLogRepository.save(
            AuditLog(
                at = Instant.now(clock),
                actorType = actorType,
                action = action,
                actorId = actorId,
                targetType = targetType,
                targetId = targetId,
                detail = detail,
                ip = ip,
            ),
        )
    }
}
