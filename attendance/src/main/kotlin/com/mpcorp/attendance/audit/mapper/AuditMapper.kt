package com.mpcorp.attendance.audit.mapper

import com.mpcorp.attendance.audit.dto.AuditLogResponse
import com.mpcorp.attendance.audit.entity.AuditLog
import org.springframework.stereotype.Component

@Component
class AuditMapper {

    fun toResponse(log: AuditLog): AuditLogResponse = AuditLogResponse(
        id = requireNotNull(log.id) { "Audit log id must not be null after persistence" },
        at = log.at,
        actorType = log.actorType,
        action = log.action,
        actorId = log.actorId,
        targetType = log.targetType,
        targetId = log.targetId,
        detail = log.detail,
        ip = log.ip,
    )
}
