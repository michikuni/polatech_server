package com.mpcorp.attendance.audit.dto

import com.mpcorp.attendance.audit.entity.ActorType
import com.mpcorp.attendance.audit.entity.AuditAction
import java.time.Instant

data class AuditLogResponse(
    val id: Long,
    val at: Instant,
    val actorType: ActorType,
    val action: AuditAction,
    val actorId: String?,
    val targetType: String?,
    val targetId: String?,
    val detail: String?,
    val ip: String?,
)
