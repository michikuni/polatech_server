package com.mpcorp.attendance.employee.mapper

import com.mpcorp.attendance.employee.dto.CreateEmployeeRequest
import com.mpcorp.attendance.employee.dto.EmployeeResponse
import com.mpcorp.attendance.employee.dto.UpdateEmployeeRequest
import com.mpcorp.attendance.employee.entity.Employee
import org.springframework.stereotype.Component

/** Hand-written Employee <-> DTO mapping (no MapStruct). Stateless component. */
@Component
class EmployeeMapper {

    fun toEntity(request: CreateEmployeeRequest): Employee = Employee(
        employeeCode = request.employeeCode.trim(),
        fullName = request.fullName.trim(),
        position = request.position.trim(),
        rank = request.rank.trim(),
    )

    fun applyUpdate(employee: Employee, request: UpdateEmployeeRequest) {
        employee.fullName = request.fullName.trim()
        employee.position = request.position.trim()
        employee.rank = request.rank.trim()
    }

    fun toResponse(employee: Employee): EmployeeResponse = EmployeeResponse(
        id = requireNotNull(employee.id) { "Employee id must not be null after persistence" },
        employeeCode = employee.employeeCode,
        fullName = employee.fullName,
        position = employee.position,
        rank = employee.rank,
        active = employee.active,
        createdAt = employee.createdAt,
        updatedAt = employee.updatedAt,
    )
}
