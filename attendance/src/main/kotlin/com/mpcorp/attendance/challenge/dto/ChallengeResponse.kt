package com.mpcorp.attendance.challenge.dto

import java.time.Instant

data class ChallengeResponse(
    val challengeId: Long,
    val challenge: String,
    val expiresAt: Instant,
)
