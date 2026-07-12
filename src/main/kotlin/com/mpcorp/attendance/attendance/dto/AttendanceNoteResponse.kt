package com.mpcorp.attendance.attendance.dto

/** The saved shift-handover note for an attendance event. */
data class AttendanceNoteResponse(
    val eventId: Long,
    val note: String,
)
