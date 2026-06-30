package com.mpcorp.attendance.attendance.controller

import com.mpcorp.attendance.attendance.dto.AttendanceEventResponse
import com.mpcorp.attendance.attendance.dto.AttendanceRequest
import com.mpcorp.attendance.attendance.dto.DailyHistoryResponse
import com.mpcorp.attendance.attendance.service.AttendanceQueryService
import com.mpcorp.attendance.attendance.service.AttendanceService
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
}
