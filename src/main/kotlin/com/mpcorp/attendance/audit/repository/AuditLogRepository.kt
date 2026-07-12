package com.mpcorp.attendance.audit.repository

import com.mpcorp.attendance.audit.entity.ActorType
import com.mpcorp.attendance.audit.entity.AuditAction
import com.mpcorp.attendance.audit.entity.AuditLog
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface AuditLogRepository : JpaRepository<AuditLog, Long> {

    @Query(
        """
        select a from AuditLog a
        where (:actorType is null or a.actorType = :actorType)
          and (:action is null or a.action = :action)
          and (:from is null or a.at >= :from)
          and (:to is null or a.at < :to)
        order by a.at desc
        """,
    )
    fun search(
        @Param("actorType") actorType: ActorType?,
        @Param("action") action: AuditAction?,
        @Param("from") from: Instant?,
        @Param("to") to: Instant?,
        pageable: Pageable,
    ): Page<AuditLog>
}
