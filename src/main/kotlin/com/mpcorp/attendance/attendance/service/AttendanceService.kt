package com.mpcorp.attendance.attendance.service

import com.mpcorp.attendance.attendance.dto.AttendanceEventResponse
import com.mpcorp.attendance.attendance.dto.AttendanceRequest
import com.mpcorp.attendance.attendance.entity.AttendanceEvent
import com.mpcorp.attendance.attendance.entity.AttendanceType
import com.mpcorp.attendance.attendance.mapper.AttendanceMapper
import com.mpcorp.attendance.attendance.repository.AttendanceEventRepository
import com.mpcorp.attendance.audit.entity.ActorType
import com.mpcorp.attendance.audit.entity.AuditAction
import com.mpcorp.attendance.audit.service.AuditService
import com.mpcorp.attendance.challenge.service.ChallengeService
import com.mpcorp.attendance.common.crypto.Base64Utils
import com.mpcorp.attendance.common.crypto.PublicKeyParser
import com.mpcorp.attendance.common.crypto.SignatureVerifier
import com.mpcorp.attendance.common.exception.BusinessRuleException
import com.mpcorp.attendance.common.exception.InvalidSignatureException
import com.mpcorp.attendance.common.exception.ResourceNotFoundException
import com.mpcorp.attendance.device.entity.DeviceStatus
import com.mpcorp.attendance.device.repository.DeviceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@Service
class AttendanceService(
    private val challengeService: ChallengeService,
    private val deviceRepository: DeviceRepository,
    private val publicKeyParser: PublicKeyParser,
    private val signatureVerifier: SignatureVerifier,
    private val attendanceEventRepository: AttendanceEventRepository,
    private val attendanceMapper: AttendanceMapper,
    private val auditService: AuditService,
    private val businessZone: ZoneId,
    private val clock: Clock,
) {

    @Transactional
    fun record(request: AttendanceRequest, sourceIp: String?): AttendanceEventResponse {
        val now = Instant.now(clock)
        val type = request.type!!

        // 1. The challenge must still be usable (bound to a device, not consumed/expired).
        val challenge = challengeService.findActiveChallenge(request.challengeId!!, now)

        // 2. The device behind the challenge must still be active.
        val device = deviceRepository.findById(challenge.deviceId)
            .orElseThrow { ResourceNotFoundException("Device ${challenge.deviceId} not found") }
        if (device.status != DeviceStatus.ACTIVE) {
            throw BusinessRuleException("Device is not active")
        }

        // 3. Verify the signature over (challenge bytes || type) BEFORE consuming
        //    the challenge — a bad signature must not burn a valid challenge.
        val signedMessage = Base64Utils.decode(challenge.challenge) + type.signedSuffix()
        val signatureBytes = decodeSignature(request.signature)
        val publicKey = publicKeyParser.parse(device.publicKey)
        if (!signatureVerifier.verify(publicKey, signedMessage, signatureBytes)) {
            auditService.record(
                actorType = ActorType.DEVICE,
                action = AuditAction.SIGNATURE_VERIFY_FAILED,
                actorId = device.employeeId.toString(),
                targetType = "DEVICE",
                targetId = device.id?.toString(),
                detail = "challengeId=${challenge.id}",
                ip = sourceIp,
            )
            throw InvalidSignatureException("Attendance signature is invalid")
        }

        // 4. Enforce check-in/check-out alternation within the business day: a
        //    check-in is only allowed when the previous state is "checked out"
        //    (last punch today is CHECK_OUT or there is none), and a check-out
        //    only when there is an open check-in. Checked before consuming the
        //    challenge so a rejected punch does not burn it.
        assertValidTransition(device.employeeId, type, now)

        // 5. Atomically claim the challenge (exactly-once; protects against replay/race).
        if (!challengeService.markConsumed(challenge.id!!, now)) {
            throw BusinessRuleException("Challenge already used")
        }

        // 6. Record the event and mark the device as recently used.
        val event = AttendanceEvent(
            employeeId = device.employeeId,
            deviceId = device.id!!,
            type = type,
            eventTime = now,
            challengeId = challenge.id!!,
            sourceIp = sourceIp,
        )
        val saved = attendanceEventRepository.save(event)
        device.lastUsedAt = now
        auditService.record(
            actorType = ActorType.DEVICE,
            action = AuditAction.ATTENDANCE_RECORDED,
            actorId = device.employeeId.toString(),
            targetType = "ATTENDANCE",
            targetId = saved.id?.toString(),
            detail = type.name,
            ip = sourceIp,
        )
        return attendanceMapper.toResponse(saved)
    }

    /**
     * Rejects a punch that would break the check-in → check-out alternation for
     * the employee's current business day. Considers only events on or after the
     * start of today (in [businessZone]); a fresh day always starts "checked out".
     */
    private fun assertValidTransition(employeeId: Long, type: AttendanceType, now: Instant) {
        val startOfDay = now.atZone(businessZone).toLocalDate().atStartOfDay(businessZone).toInstant()
        val lastType = attendanceEventRepository
            .findFirstByEmployeeIdAndEventTimeGreaterThanEqualOrderByEventTimeDesc(employeeId, startOfDay)
            ?.type
        when (type) {
            AttendanceType.CHECK_IN ->
                if (lastType == AttendanceType.CHECK_IN) {
                    throw BusinessRuleException("Đã check-in, cần check-out trước khi check-in lại")
                }

            AttendanceType.CHECK_OUT ->
                if (lastType != AttendanceType.CHECK_IN) {
                    throw BusinessRuleException("Chưa check-in, không thể check-out")
                }
        }
    }

    private fun AttendanceType.signedSuffix(): ByteArray = name.toByteArray(StandardCharsets.UTF_8)

    private fun decodeSignature(signature: String): ByteArray = try {
        Base64Utils.decode(signature)
    } catch (ex: IllegalArgumentException) {
        throw InvalidSignatureException("Signature is not valid Base64")
    }
}
