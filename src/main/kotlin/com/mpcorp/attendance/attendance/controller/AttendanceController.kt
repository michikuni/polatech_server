package com.mpcorp.attendance.attendance.controller

import com.mpcorp.attendance.attendance.dto.AttendanceEventResponse
import com.mpcorp.attendance.attendance.dto.AttendanceNoteRequest
import com.mpcorp.attendance.attendance.dto.AttendanceNoteResponse
import com.mpcorp.attendance.attendance.dto.AttendanceRequest
import com.mpcorp.attendance.attendance.dto.AttendanceStatusResponse
import com.mpcorp.attendance.attendance.dto.DailyHistoryResponse
import com.mpcorp.attendance.attendance.service.AttendanceQueryService
import com.mpcorp.attendance.attendance.service.AttendanceService
import com.mpcorp.attendance.attendance.service.ShiftNoteService
import com.mpcorp.attendance.common.response.ApiResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** Public endpoints a device uses: submit a signed punch and read its own history. */
@RestController
@RequestMapping("/api/attendance")
class AttendanceController(
    private val attendanceService: AttendanceService,
    private val attendanceQueryService: AttendanceQueryService,
    private val shiftNoteService: ShiftNoteService,
) {

    @PostMapping
    fun record(
        @Valid @RequestBody request: AttendanceRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<ApiResponse<AttendanceEventResponse>> {
        val response = attendanceService.record(request, httpRequest.remoteAddr)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response))
    }

    /** Daily check-in/out history for the device's own employee (LAN, device-scoped read). */
    @GetMapping("/history")
    fun history(
        @RequestParam deviceId: Long,
        @RequestParam(required = false, defaultValue = "30") days: Int,
    ): ResponseEntity<ApiResponse<List<DailyHistoryResponse>>> =
        ResponseEntity.ok(ApiResponse.ok(attendanceQueryService.deviceHistory(deviceId, days)))

    /** Current check-in/check-out state for today, so the device can enable only the valid action. */
    @GetMapping("/status")
    fun status(
        @RequestParam deviceId: Long,
    ): ResponseEntity<ApiResponse<AttendanceStatusResponse>> =
        ResponseEntity.ok(ApiResponse.ok(attendanceQueryService.deviceStatus(deviceId)))

    /** Attaches the one-time shift-handover note to a check-in (write-once). */
    @PostMapping("/note")
    fun addNote(
        @Valid @RequestBody request: AttendanceNoteRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<ApiResponse<AttendanceNoteResponse>> =
        ResponseEntity.ok(ApiResponse.ok(shiftNoteService.addNote(request, httpRequest.remoteAddr)))
}
