package com.mpcorp.attendance.attendance.controller

import com.mpcorp.attendance.attendance.dto.AttendanceEventResponse
import com.mpcorp.attendance.attendance.dto.DailySummaryResponse
import com.mpcorp.attendance.attendance.service.AttendanceQueryService
import com.mpcorp.attendance.attendance.service.AttendanceReportService
import com.mpcorp.attendance.attendance.service.ReportPeriod
import com.mpcorp.attendance.common.response.ApiResponse
import com.mpcorp.attendance.common.response.PageResponse
import org.springframework.core.io.ByteArrayResource
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
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
    private val attendanceReportService: AttendanceReportService,
) {

    /** [hasNote] narrows to punches that carry a shift-handover note; omit for all. */
    @GetMapping
    fun list(
        @RequestParam(required = false) employeeId: Long?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fromDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) toDate: LocalDate?,
        @RequestParam(required = false) hasNote: Boolean?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ResponseEntity<ApiResponse<PageResponse<AttendanceEventResponse>>> =
        ResponseEntity.ok(ApiResponse.ok(attendanceQueryService.list(employeeId, fromDate, toDate, hasNote, pageable)))

    @GetMapping("/daily-summary")
    fun dailySummary(
        @RequestParam employeeId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
    ): ResponseEntity<ApiResponse<DailySummaryResponse>> =
        ResponseEntity.ok(ApiResponse.ok(attendanceQueryService.dailySummary(employeeId, date)))

    /**
     * Exports attendance punches to an .xlsx file. [period] (DAY/WEEK/MONTH) and
     * [date] define the window; [employeeCodes] is a comma-separated officer-code
     * list, or blank/absent to export every employee.
     */
    @GetMapping("/report")
    fun report(
        @RequestParam period: ReportPeriod,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @RequestParam(required = false) employeeCodes: String?,
    ): ResponseEntity<ByteArrayResource> {
        val codes = employeeCodes
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
        val file = attendanceReportService.buildExcel(period, date, codes)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${file.filename}\"")
            .body(ByteArrayResource(file.bytes))
    }
}
