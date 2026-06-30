package com.mpcorp.attendance.employee.service

import com.mpcorp.attendance.audit.entity.AuditAction
import com.mpcorp.attendance.audit.service.AuditService
import com.mpcorp.attendance.common.exception.BusinessRuleException
import com.mpcorp.attendance.common.exception.ResourceNotFoundException
import com.mpcorp.attendance.common.response.PageResponse
import com.mpcorp.attendance.employee.dto.CreateEmployeeRequest
import com.mpcorp.attendance.employee.dto.EmployeeResponse
import com.mpcorp.attendance.employee.dto.UpdateEmployeeRequest
import com.mpcorp.attendance.employee.entity.Employee
import com.mpcorp.attendance.employee.mapper.EmployeeMapper
import com.mpcorp.attendance.employee.repository.EmployeeRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EmployeeService(
    private val employeeRepository: EmployeeRepository,
    private val employeeMapper: EmployeeMapper,
    private val auditService: AuditService,
) {

    @Transactional
    fun create(request: CreateEmployeeRequest): EmployeeResponse {
        val code = request.employeeCode.trim()
        if (employeeRepository.existsByEmployeeCode(code)) {
            throw BusinessRuleException("Employee code '$code' already exists")
        }
        val saved = employeeRepository.save(employeeMapper.toEntity(request))
        auditService.recordAdminAction(AuditAction.EMPLOYEE_CREATED, "EMPLOYEE", saved.id?.toString())
        return employeeMapper.toResponse(saved)
    }

    @Transactional(readOnly = true)
    fun list(active: Boolean?, q: String?, pageable: Pageable): PageResponse<EmployeeResponse> {
        val normalizedQuery = q?.trim()?.takeIf { it.isNotEmpty() }
        val page = employeeRepository.search(active, normalizedQuery, pageable)
            .map(employeeMapper::toResponse)
        return PageResponse.from(page)
    }

    @Transactional(readOnly = true)
    fun get(id: Long): EmployeeResponse = employeeMapper.toResponse(findOrThrow(id))

    @Transactional
    fun update(id: Long, request: UpdateEmployeeRequest): EmployeeResponse {
        val employee = findOrThrow(id)
        employeeMapper.applyUpdate(employee, request)
        auditService.recordAdminAction(AuditAction.EMPLOYEE_UPDATED, "EMPLOYEE", id.toString())
        return employeeMapper.toResponse(employee)
    }

    @Transactional
    fun setActive(id: Long, active: Boolean): EmployeeResponse {
        val employee = findOrThrow(id)
        employee.active = active
        auditService.recordAdminAction(AuditAction.EMPLOYEE_ACTIVE_CHANGED, "EMPLOYEE", id.toString(), "active=$active")
        return employeeMapper.toResponse(employee)
    }

    private fun findOrThrow(id: Long): Employee = employeeRepository.findById(id)
        .orElseThrow { ResourceNotFoundException("Employee $id not found") }
}
