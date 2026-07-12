package com.mpcorp.attendance.challenge.service

import com.mpcorp.attendance.challenge.dto.ChallengeResponse
import com.mpcorp.attendance.challenge.entity.Challenge
import com.mpcorp.attendance.challenge.mapper.ChallengeMapper
import com.mpcorp.attendance.challenge.repository.ChallengeRepository
import com.mpcorp.attendance.common.crypto.Base64Utils
import com.mpcorp.attendance.common.crypto.ChallengeGenerator
import com.mpcorp.attendance.common.crypto.CryptoConstants
import com.mpcorp.attendance.common.exception.BusinessRuleException
import com.mpcorp.attendance.common.exception.ResourceNotFoundException
import com.mpcorp.attendance.device.entity.DeviceStatus
import com.mpcorp.attendance.device.repository.DeviceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class ChallengeService(
    private val challengeRepository: ChallengeRepository,
    private val deviceRepository: DeviceRepository,
    private val challengeGenerator: ChallengeGenerator,
    private val challengeMapper: ChallengeMapper,
    private val clock: Clock,
) {

    /** Issues a fresh challenge for an ACTIVE device. */
    @Transactional
    fun issue(deviceId: Long): ChallengeResponse {
        val device = deviceRepository.findById(deviceId)
            .orElseThrow { ResourceNotFoundException("Device $deviceId not found") }
        if (device.status != DeviceStatus.ACTIVE) {
            throw BusinessRuleException("Device is not active")
        }

        val challenge = Challenge(
            deviceId = deviceId,
            challenge = Base64Utils.encode(challengeGenerator.generate()),
            expiresAt = Instant.now(clock).plusSeconds(CryptoConstants.EXPIRE_SECONDS.toLong()),
        )
        return challengeMapper.toResponse(challengeRepository.save(challenge))
    }

    /** Loads a challenge only if it is still usable (not consumed, not expired). */
    @Transactional(readOnly = true)
    fun findActiveChallenge(challengeId: Long, now: Instant): Challenge {
        val challenge = challengeRepository.findById(challengeId)
            .orElseThrow { ResourceNotFoundException("Challenge $challengeId not found") }
        if (challenge.consumedAt != null) throw BusinessRuleException("Challenge already used")
        if (now.isAfter(challenge.expiresAt)) throw BusinessRuleException("Challenge has expired")
        return challenge
    }

    /** Atomically claims the challenge. Returns true if this call consumed it. */
    @Transactional
    fun markConsumed(challengeId: Long, now: Instant): Boolean =
        challengeRepository.consume(challengeId, now) == 1
}
