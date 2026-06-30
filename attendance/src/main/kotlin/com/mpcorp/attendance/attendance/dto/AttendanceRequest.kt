package com.mpcorp.attendance.attendance.dto

import com.mpcorp.attendance.attendance.entity.AttendanceType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class AttendanceRequest(

    @field:NotNull
    val challengeId: Long?,

    @field:NotNull
    val type: AttendanceType?,

    @field:NotBlank
    val signature: String,
)
