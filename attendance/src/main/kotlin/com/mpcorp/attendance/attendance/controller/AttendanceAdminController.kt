package com.mpcorp.attendance.attendance.controller

import com.mpcorp.attendance.attendance.dto.AttendanceEventResponse
import com.mpcorp.attendance.attendance.dto.DailySummaryResponse
import com.mpcorp.attendance.attendance.service.AttendanceQueryService
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

/** Admin read views over attendance. */
@RestController
@RequestMapping("/api/admin/attendance")
class AttendanceAdminController(
    private val attendanceQueryService: AttendanceQueryService,
) {

    @GetMapping
    fun list(
        @RequestParam(required = false) employeeId: Long?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fromDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) toDate: LocalDate?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ResponseEntity<ApiResponse<PageResponse<AttendanceEventResponse>>> =
        ResponseEntity.ok(ApiResponse.ok(attendanceQueryService.list(employeeId, fromDate, toDate, pageable)))

    @GetMapping("/daily-summary")
    fun dailySummary(
        @RequestParam employeeId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
    ): ResponseEntity<ApiResponse<DailySummaryResponse>> =
        ResponseEntity.ok(ApiResponse.ok(attendanceQueryService.dailySummary(employeeId, date)))
}
