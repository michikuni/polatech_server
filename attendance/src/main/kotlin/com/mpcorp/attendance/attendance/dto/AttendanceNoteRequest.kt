package com.mpcorp.attendance.attendance.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/**
 * Attaches a one-time shift-handover note to the check-in [eventId], made by the
 * device [deviceId]. The note is write-once; a second attempt is rejected.
 */
data class AttendanceNoteRequest(

    @field:NotNull
    val deviceId: Long?,

    @field:NotNull
    val eventId: Long?,

    @field:NotBlank
    @field:Size(max = 1000)
    val note: String?,
)
