package com.mpcorp.attendance.challenge.mapper

import com.mpcorp.attendance.challenge.dto.ChallengeResponse
import com.mpcorp.attendance.challenge.entity.Challenge
import org.springframework.stereotype.Component

@Component
class ChallengeMapper {

    fun toResponse(challenge: Challenge): ChallengeResponse = ChallengeResponse(
        challengeId = requireNotNull(challenge.id) { "Challenge id must not be null after persistence" },
        challenge = challenge.challenge,
        expiresAt = challenge.expiresAt,
    )
}
