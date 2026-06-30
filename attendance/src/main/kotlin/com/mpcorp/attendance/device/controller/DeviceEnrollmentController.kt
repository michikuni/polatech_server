package com.mpcorp.attendance.device.controller

import com.mpcorp.attendance.common.response.ApiResponse
import com.mpcorp.attendance.device.dto.EnrollDeviceRequest
import com.mpcorp.attendance.device.dto.EnrollDeviceResponse
import com.mpcorp.attendance.device.service.EnrollmentService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Public endpoint: a device enrolls itself with a one-time pairing code. */
@RestController
@RequestMapping("/api/devices")
class DeviceEnrollmentController(
    private val enrollmentService: EnrollmentService,
) {

    @PostMapping("/enroll")
    fun enroll(@Valid @RequestBody request: EnrollDeviceRequest): ResponseEntity<ApiResponse<EnrollDeviceResponse>> =
        ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(enrollmentService.enroll(request)))
}
