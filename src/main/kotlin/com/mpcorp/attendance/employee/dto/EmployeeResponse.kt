package com.mpcorp.attendance.employee.dto

import java.time.Instant

data class EmployeeResponse(
    val id: Long,
    val employeeCode: String,
    val fullName: String,
    val position: String,
    val rank: String,
    val active: Boolean,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)
