package com.mpcorp.attendance.challenge.service

import com.mpcorp.attendance.challenge.entity.Challenge
import com.mpcorp.attendance.challenge.mapper.ChallengeMapper
import com.mpcorp.attendance.challenge.repository.ChallengeRepository
import com.mpcorp.attendance.common.crypto.Base64Utils
import com.mpcorp.attendance.common.crypto.SecureRandomChallengeGenerator
import com.mpcorp.attendance.common.exception.BusinessRuleException
import com.mpcorp.attendance.common.exception.ResourceNotFoundException
import com.mpcorp.attendance.device.entity.Device
import com.mpcorp.attendance.device.entity.DevicePlatform
import com.mpcorp.attendance.device.entity.DeviceStatus
import com.mpcorp.attendance.device.repository.DeviceRepository
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChallengeServiceTest {

    private val challengeRepository = mock(ChallengeRepository::class.java)
    private val deviceRepository = mock(DeviceRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-06-29T00:00:00Z"), ZoneOffset.UTC)
    private val now = Instant.now(clock)

    private val service = ChallengeService(
        challengeRepository,
        deviceRepository,
        SecureRandomChallengeGenerator(),
        ChallengeMapper(),
        clock,
    )

    private fun device(status: DeviceStatus = DeviceStatus.ACTIVE) = Device(
        employeeId = 5L,
        publicKey = "key",
        publicKeyFingerprint = "fp",
        platform = DevicePlatform.IOS,
        status = status,
        enrolledAt = now,
    ).apply { id = 3L }

    private fun challenge(consumedAt: Instant? = null, expiresAt: Instant = now.plusSeconds(60)) =
        Challenge(deviceId = 3L, challenge = Base64Utils.encode(ByteArray(32)), expiresAt = expiresAt, consumedAt = consumedAt)
            .apply { id = 11L }

    // --- issue ---

    @Test
    fun `issue creates a 32-byte challenge for an active device`() {
        given(deviceRepository.findById(3L)).willReturn(Optional.of(device()))
        given(challengeRepository.save(any())).willAnswer { it.getArgument<Challenge>(0).apply { id = 11L } }

        val result = service.issue(3L)

        assertEquals(11L, result.challengeId)
        assertEquals(now.plusSeconds(60), result.expiresAt)
        assertEquals(32, Base64Utils.decode(result.challenge).size)
    }

    @Test
    fun `issue rejects an inactive device`() {
        given(deviceRepository.findById(3L)).willReturn(Optional.of(device(status = DeviceStatus.REVOKED)))
        assertFailsWith<BusinessRuleException> { service.issue(3L) }
    }

    @Test
    fun `issue rejects an unknown device`() {
        given(deviceRepository.findById(9L)).willReturn(Optional.empty())
        assertFailsWith<ResourceNotFoundException> { service.issue(9L) }
    }

    // --- findActiveChallenge ---

    @Test
    fun `findActiveChallenge returns a usable challenge`() {
        given(challengeRepository.findById(11L)).willReturn(Optional.of(challenge()))
        assertEquals(11L, service.findActiveChallenge(11L, now).id)
    }

    @Test
    fun `findActiveChallenge rejects a consumed challenge`() {
        given(challengeRepository.findById(11L)).willReturn(Optional.of(challenge(consumedAt = now)))
        assertFailsWith<BusinessRuleException> { service.findActiveChallenge(11L, now) }
    }

    @Test
    fun `findActiveChallenge rejects an expired challenge`() {
        given(challengeRepository.findById(11L)).willReturn(Optional.of(challenge(expiresAt = now.minusSeconds(1))))
        assertFailsWith<BusinessRuleException> { service.findActiveChallenge(11L, now) }
    }

    @Test
    fun `findActiveChallenge throws when the challenge is missing`() {
        given(challengeRepository.findById(99L)).willReturn(Optional.empty())
        assertFailsWith<ResourceNotFoundException> { service.findActiveChallenge(99L, now) }
    }

    // --- markConsumed ---

    @Test
    fun `markConsumed is true when the claim wins`() {
        given(challengeRepository.consume(11L, now)).willReturn(1)
        assertTrue(service.markConsumed(11L, now))
    }

    @Test
    fun `markConsumed is false when already consumed`() {
        given(challengeRepository.consume(11L, now)).willReturn(0)
        assertFalse(service.markConsumed(11L, now))
    }
}
