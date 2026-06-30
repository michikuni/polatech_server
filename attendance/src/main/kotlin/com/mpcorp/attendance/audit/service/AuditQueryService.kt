package com.mpcorp.attendance.audit.service

import com.mpcorp.attendance.audit.dto.AuditLogResponse
import com.mpcorp.attendance.audit.entity.ActorType
import com.mpcorp.attendance.audit.entity.AuditAction
import com.mpcorp.attendance.audit.mapper.AuditMapper
import com.mpcorp.attendance.audit.repository.AuditLogRepository
import com.mpcorp.attendance.common.response.PageResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId

@Service
class AuditQueryService(
    private val auditLogRepository: AuditLogRepository,
    private val auditMapper: AuditMapper,
    private val businessZone: ZoneId,
) {

    @Transactional(readOnly = true)
    fun list(
        actorType: ActorType?,
        action: AuditAction?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        pageable: Pageable,
    ): PageResponse<AuditLogResponse> {
        val from = fromDate?.atStartOfDay(businessZone)?.toInstant()
        val to = toDate?.plusDays(1)?.atStartOfDay(businessZone)?.toInstant()
        val page = auditLogRepository.search(actorType, action, from, to, pageable)
            .map(auditMapper::toResponse)
        return PageResponse.from(page)
    }
}
