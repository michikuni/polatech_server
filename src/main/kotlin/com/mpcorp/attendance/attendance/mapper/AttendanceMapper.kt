package com.mpcorp.attendance.attendance.mapper

import com.mpcorp.attendance.attendance.dto.AttendanceEventResponse
import com.mpcorp.attendance.attendance.entity.AttendanceEvent
import org.springframework.stereotype.Component

@Component
class AttendanceMapper {

    fun toResponse(event: AttendanceEvent, employeeName: String? = null): AttendanceEventResponse = AttendanceEventResponse(
        id = requireNotNull(event.id) { "Attendance event id must not be null after persistence" },
        employeeId = event.employeeId,
        employeeName = employeeName,
        deviceId = event.deviceId,
        type = event.type,
        eventTime = event.eventTime,
        note = event.note,
    )
}
