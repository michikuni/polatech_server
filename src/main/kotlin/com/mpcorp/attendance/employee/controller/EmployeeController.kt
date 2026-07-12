package com.mpcorp.attendance.employee.controller

import com.mpcorp.attendance.common.response.ApiResponse
import com.mpcorp.attendance.common.response.PageResponse
import com.mpcorp.attendance.employee.dto.CreateEmployeeRequest
import com.mpcorp.attendance.employee.dto.EmployeeResponse
import com.mpcorp.attendance.employee.dto.SetActiveRequest
import com.mpcorp.attendance.employee.dto.UpdateEmployeeRequest
import com.mpcorp.attendance.employee.service.EmployeeService
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/employees")
class EmployeeController(
    private val employeeService: EmployeeService,
) {

    @PostMapping
    fun create(@Valid @RequestBody request: CreateEmployeeRequest): ResponseEntity<ApiResponse<EmployeeResponse>> =
        ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(employeeService.create(request)))

    @GetMapping
    fun list(
        @RequestParam(required = false) active: Boolean?,
        @RequestParam(required = false) q: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ResponseEntity<ApiResponse<PageResponse<EmployeeResponse>>> =
        ResponseEntity.ok(ApiResponse.ok(employeeService.list(active, q, pageable)))

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): ResponseEntity<ApiResponse<EmployeeResponse>> =
        ResponseEntity.ok(ApiResponse.ok(employeeService.get(id)))

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateEmployeeRequest,
    ): ResponseEntity<ApiResponse<EmployeeResponse>> =
        ResponseEntity.ok(ApiResponse.ok(employeeService.update(id, request)))

    @PatchMapping("/{id}/active")
    fun setActive(
        @PathVariable id: Long,
        @Valid @RequestBody request: SetActiveRequest,
    ): ResponseEntity<ApiResponse<EmployeeResponse>> =
        ResponseEntity.ok(ApiResponse.ok(employeeService.setActive(id, request.active!!)))

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long): ResponseEntity<ApiResponse<Unit>> {
        employeeService.delete(id)
        return ResponseEntity.ok(ApiResponse.ok(Unit))
    }
}
