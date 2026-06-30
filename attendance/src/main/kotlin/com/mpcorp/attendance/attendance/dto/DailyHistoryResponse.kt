package com.mpcorp.attendance.attendance.dto

import java.time.Instant
import java.time.LocalDate

/**
 * One day of attendance for a device's employee: [checkIn] is the earliest punch
 * of the day (vào ca), [checkOut] the latest (ra ca). A day only appears when it
 * has at least one event, so both are non-null in practice.
 */
data class DailyHistoryResponse(
    val date: LocalDate,
    val checkIn: Instant?,
    val checkOut: Instant?,
)
