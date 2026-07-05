package com.mpcorp.attendance.attendance.dto

import com.mpcorp.attendance.attendance.entity.AttendanceType
import java.time.Instant

/**
 * The current attendance state of a device's employee for today, so a client can
 * enable/disable the check-in/check-out buttons in step with the server's
 * alternation rule. [openSession] is true when the last punch today is a
 * CHECK_IN (i.e. a check-out is the only valid next punch).
 */
data class AttendanceStatusResponse(
    val openSession: Boolean,
    val lastType: AttendanceType?,
    val lastEventTime: Instant?,
)
