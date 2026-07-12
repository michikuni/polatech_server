package com.mpcorp.attendance.attendance.dto

import java.time.Instant
import java.time.LocalDate

data class DailySummaryResponse(
    val employeeId: Long,
    val date: LocalDate,
    val workedSeconds: Long,
    val firstCheckIn: Instant?,
    val lastCheckOut: Instant?,
    val checkInCount: Int,
    val checkOutCount: Int,
    /** True if the last check-in has no matching check-out (still "in"). */
    val openSession: Boolean,
)
