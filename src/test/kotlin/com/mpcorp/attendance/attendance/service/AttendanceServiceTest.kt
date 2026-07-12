package com.mpcorp.attendance.attendance.service

import com.mpcorp.attendance.attendance.dto.AttendanceRequest
import com.mpcorp.attendance.attendance.entity.AttendanceEvent
import com.mpcorp.attendance.attendance.entity.AttendanceType
import com.mpcorp.attendance.attendance.mapper.AttendanceMapper
import com.mpcorp.attendance.attendance.repository.AttendanceEventRepository
import com.mpcorp.attendance.audit.service.AuditService
import com.mpcorp.attendance.challenge.entity.Challenge
import com.mpcorp.attendance.challenge.service.ChallengeService
import com.mpcorp.attendance.common.crypto.Base64Utils
import com.mpcorp.attendance.common.crypto.CryptoTestSupport
import com.mpcorp.attendance.common.crypto.PublicKeyParser
import com.mpcorp.attendance.common.crypto.SignatureVerifier
import com.mpcorp.attendance.common.exception.BusinessRuleException
import com.mpcorp.attendance.common.exception.InvalidSignatureException
import com.mpcorp.attendance.device.entity.Device
import com.mpcorp.attendance.device.entity.DevicePlatform
import com.mpcorp.attendance.device.entity.DeviceStatus
import com.mpcorp.attendance.device.repository.DeviceRepository
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AttendanceServiceTest {

    private val challengeService = mock(ChallengeService::class.java)
    private val deviceRepository = mock(DeviceRepository::class.java)
    private val attendanceEventRepository = mock(AttendanceEventRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-06-29T03:00:00Z"), ZoneOffset.UTC)
    private val now = Instant.now(clock)
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")
    private val startOfDay = now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()

    private val service = AttendanceService(
        challengeService,
        deviceRepository,
        PublicKeyParser(),
        SignatureVerifier(),
        attendanceEventRepository,
        AttendanceMapper(),
        mock(AuditService::class.java),
        zone,
        clock,
    )

    private val challengeBytes = ByteArray(32) { it.toByte() }
    private val challengeB64 = Base64Utils.encode(challengeBytes)

    private fun challenge() =
        Challenge(deviceId = 3L, challenge = challengeB64, expiresAt = now.plusSeconds(60)).apply { id = 11L }

    private fun device(keyPair: KeyPair, status: DeviceStatus = DeviceStatus.ACTIVE) = Device(
        employeeId = 5L,
        publicKey = Base64Utils.encode(keyPair.public.encoded),
        publicKeyFingerprint = "fp",
        platform = DevicePlatform.IOS,
        status = status,
        enrolledAt = now,
    ).apply { id = 3L }

    private fun signature(keyPair: KeyPair, type: AttendanceType): String {
        val message = challengeBytes + type.name.toByteArray(StandardCharsets.UTF_8)
        return Base64Utils.encode(CryptoTestSupport.sign(message, keyPair.private))
    }

    /** Stubs the "last punch of the day" the transition check reads (employee 5, start of today). */
    private fun givenLastPunch(type: AttendanceType?) {
        val last = type?.let {
            AttendanceEvent(employeeId = 5L, deviceId = 3L, type = it, eventTime = now.minusSeconds(3600))
                .apply { id = 99L }
        }
        given(
            attendanceEventRepository
                .findFirstByEmployeeIdAndEventTimeGreaterThanEqualOrderByEventTimeDesc(5L, startOfDay),
        ).willReturn(last)
    }

    @Test
    fun `record stores an event for a valid signature`() {
        val keyPair = CryptoTestSupport.generateEcKeyPair()
        val device = device(keyPair)
        given(challengeService.findActiveChallenge(11L, now)).willReturn(challenge())
        given(deviceRepository.findById(3L)).willReturn(Optional.of(device))
        given(challengeService.markConsumed(11L, now)).willReturn(true)
        given(attendanceEventRepository.save(any())).willAnswer { it.getArgument<AttendanceEvent>(0).apply { id = 100L } }

        val result = service.record(
            AttendanceRequest(11L, AttendanceType.CHECK_IN, signature(keyPair, AttendanceType.CHECK_IN)),
            "10.0.0.5",
        )

        assertEquals(100L, result.id)
        assertEquals(5L, result.employeeId)
        assertEquals(AttendanceType.CHECK_IN, result.type)
        assertEquals(now, result.eventTime)
        assertEquals(now, device.lastUsedAt)
    }

    @Test
    fun `record rejects a signature from a different key and does not consume the challenge`() {
        val keyPair = CryptoTestSupport.generateEcKeyPair()
        val attacker = CryptoTestSupport.generateEcKeyPair()
        given(challengeService.findActiveChallenge(11L, now)).willReturn(challenge())
        given(deviceRepository.findById(3L)).willReturn(Optional.of(device(keyPair)))

        assertFailsWith<InvalidSignatureException> {
            service.record(
                AttendanceRequest(11L, AttendanceType.CHECK_IN, signature(attacker, AttendanceType.CHECK_IN)),
                null,
            )
        }
        // The InvalidSignatureException above proves we stopped before consuming the challenge
        // (a consumed-but-lost claim would instead be a BusinessRuleException).
    }

    @Test
    fun `record rejects when the type is tampered after signing`() {
        val keyPair = CryptoTestSupport.generateEcKeyPair()
        given(challengeService.findActiveChallenge(11L, now)).willReturn(challenge())
        given(deviceRepository.findById(3L)).willReturn(Optional.of(device(keyPair)))

        // Signed for CHECK_IN but submitted as CHECK_OUT -> message mismatch -> invalid.
        assertFailsWith<InvalidSignatureException> {
            service.record(
                AttendanceRequest(11L, AttendanceType.CHECK_OUT, signature(keyPair, AttendanceType.CHECK_IN)),
                null,
            )
        }
    }

    @Test
    fun `record rejects an inactive device`() {
        val keyPair = CryptoTestSupport.generateEcKeyPair()
        given(challengeService.findActiveChallenge(11L, now)).willReturn(challenge())
        given(deviceRepository.findById(3L)).willReturn(Optional.of(device(keyPair, status = DeviceStatus.REVOKED)))

        assertFailsWith<BusinessRuleException> {
            service.record(
                AttendanceRequest(11L, AttendanceType.CHECK_IN, signature(keyPair, AttendanceType.CHECK_IN)),
                null,
            )
        }
    }

    @Test
    fun `record rejects when the challenge claim is lost`() {
        val keyPair = CryptoTestSupport.generateEcKeyPair()
        given(challengeService.findActiveChallenge(11L, now)).willReturn(challenge())
        given(deviceRepository.findById(3L)).willReturn(Optional.of(device(keyPair)))
        given(challengeService.markConsumed(11L, now)).willReturn(false)

        assertFailsWith<BusinessRuleException> {
            service.record(
                AttendanceRequest(11L, AttendanceType.CHECK_IN, signature(keyPair, AttendanceType.CHECK_IN)),
                null,
            )
        }
        verify(attendanceEventRepository, never()).save(any())
    }

    @Test
    fun `record rejects a malformed signature`() {
        val keyPair = CryptoTestSupport.generateEcKeyPair()
        given(challengeService.findActiveChallenge(11L, now)).willReturn(challenge())
        given(deviceRepository.findById(3L)).willReturn(Optional.of(device(keyPair)))

        assertFailsWith<InvalidSignatureException> {
            service.record(AttendanceRequest(11L, AttendanceType.CHECK_IN, "!!not-base64!!"), null)
        }
    }

    @Test
    fun `record rejects a second check-in without a check-out and does not consume the challenge`() {
        val keyPair = CryptoTestSupport.generateEcKeyPair()
        given(challengeService.findActiveChallenge(11L, now)).willReturn(challenge())
        given(deviceRepository.findById(3L)).willReturn(Optional.of(device(keyPair)))
        givenLastPunch(AttendanceType.CHECK_IN)

        assertFailsWith<BusinessRuleException> {
            service.record(
                AttendanceRequest(11L, AttendanceType.CHECK_IN, signature(keyPair, AttendanceType.CHECK_IN)),
                null,
            )
        }
        verify(challengeService, never()).markConsumed(11L, now)
        verify(attendanceEventRepository, never()).save(any())
    }

    @Test
    fun `record allows a check-in when the previous punch is a check-out`() {
        val keyPair = CryptoTestSupport.generateEcKeyPair()
        val device = device(keyPair)
        given(challengeService.findActiveChallenge(11L, now)).willReturn(challenge())
        given(deviceRepository.findById(3L)).willReturn(Optional.of(device))
        given(challengeService.markConsumed(11L, now)).willReturn(true)
        given(attendanceEventRepository.save(any())).willAnswer { it.getArgument<AttendanceEvent>(0).apply { id = 100L } }
        givenLastPunch(AttendanceType.CHECK_OUT)

        val result = service.record(
            AttendanceRequest(11L, AttendanceType.CHECK_IN, signature(keyPair, AttendanceType.CHECK_IN)),
            null,
        )

        assertEquals(AttendanceType.CHECK_IN, result.type)
    }

    @Test
    fun `record rejects a check-out with no open check-in and does not consume the challenge`() {
        val keyPair = CryptoTestSupport.generateEcKeyPair()
        given(challengeService.findActiveChallenge(11L, now)).willReturn(challenge())
        given(deviceRepository.findById(3L)).willReturn(Optional.of(device(keyPair)))
        givenLastPunch(null)

        assertFailsWith<BusinessRuleException> {
            service.record(
                AttendanceRequest(11L, AttendanceType.CHECK_OUT, signature(keyPair, AttendanceType.CHECK_OUT)),
                null,
            )
        }
        verify(challengeService, never()).markConsumed(11L, now)
        verify(attendanceEventRepository, never()).save(any())
    }

    @Test
    fun `record allows a check-out after an open check-in`() {
        val keyPair = CryptoTestSupport.generateEcKeyPair()
        given(challengeService.findActiveChallenge(11L, now)).willReturn(challenge())
        given(deviceRepository.findById(3L)).willReturn(Optional.of(device(keyPair)))
        given(challengeService.markConsumed(11L, now)).willReturn(true)
        given(attendanceEventRepository.save(any())).willAnswer { it.getArgument<AttendanceEvent>(0).apply { id = 101L } }
        givenLastPunch(AttendanceType.CHECK_IN)

        val result = service.record(
            AttendanceRequest(11L, AttendanceType.CHECK_OUT, signature(keyPair, AttendanceType.CHECK_OUT)),
            null,
        )

        assertEquals(AttendanceType.CHECK_OUT, result.type)
    }
}
