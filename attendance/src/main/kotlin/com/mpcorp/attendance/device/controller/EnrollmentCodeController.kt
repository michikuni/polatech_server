package com.mpcorp.attendance.device.controller

import com.mpcorp.attendance.common.response.ApiResponse
import com.mpcorp.attendance.device.dto.IssueEnrollmentCodeResponse
import com.mpcorp.attendance.device.service.EnrollmentService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal

/** Admin issues a one-time pairing code for an employee. */
@RestController
@RequestMapping("/api/admin/employees")
class EnrollmentCodeController(
    private val enrollmentService: EnrollmentService,
) {

    @PostMapping("/{employeeId}/enrollment-codes")
    fun issue(
        @PathVariable employeeId: Long,
        principal: Principal?,
    ): ResponseEntity<ApiResponse<IssueEnrollmentCodeResponse>> {
        val response = enrollmentService.issueCode(employeeId, principal?.name)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response))
    }
}
