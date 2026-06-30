package com.mpcorp.attendance.audit.controller

import com.mpcorp.attendance.audit.dto.AuditLogResponse
import com.mpcorp.attendance.audit.entity.ActorType
import com.mpcorp.attendance.audit.entity.AuditAction
import com.mpcorp.attendance.audit.service.AuditQueryService
import com.mpcorp.attendance.common.response.ApiResponse
import com.mpcorp.attendance.common.response.PageResponse
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/admin/audit")
class AuditController(
    private val auditQueryService: AuditQueryService,
) {

    @GetMapping
    fun list(
        @RequestParam(required = false) actorType: ActorType?,
        @RequestParam(required = false) action: AuditAction?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fromDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) toDate: LocalDate?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> =
        ResponseEntity.ok(ApiResponse.ok(auditQueryService.list(actorType, action, fromDate, toDate, pageable)))
}
