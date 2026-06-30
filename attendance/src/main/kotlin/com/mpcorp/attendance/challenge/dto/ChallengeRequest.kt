package com.mpcorp.attendance.challenge.dto

import jakarta.validation.constraints.NotNull

data class ChallengeRequest(

    @field:NotNull
    val deviceId: Long?,
)
