package com.mpcorp.attendance.attendance.dto

import com.mpcorp.attendance.attendance.entity.AttendanceType
import java.time.Instant

data class AttendanceEventResponse(
    val id: Long,
    val employeeId: Long,
    val employeeName: String?,
    val deviceId: Long,
    val type: AttendanceType,
    val eventTime: Instant,
)
