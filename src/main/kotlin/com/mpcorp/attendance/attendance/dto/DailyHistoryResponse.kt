package com.mpcorp.attendance.attendance.dto

import com.mpcorp.attendance.attendance.entity.AttendanceType
import java.time.Instant
import java.time.LocalDate

/**
 * One day of attendance for a device's employee: every punch of the day in
 * chronological order (earliest first). The list grows with the number of
 * check-ins/check-outs, so the client can render one expanding row per day.
 */
data class DailyHistoryResponse(
    val date: LocalDate,
    val punches: List<DailyPunchResponse>,
)

/**
 * A single punch within a day. [note] is the shift-handover note (only ever set
 * on a check-in); when non-null the client shows it read-only.
 */
data class DailyPunchResponse(
    val id: Long,
    val type: AttendanceType,
    val eventTime: Instant,
    val note: String?,
)
