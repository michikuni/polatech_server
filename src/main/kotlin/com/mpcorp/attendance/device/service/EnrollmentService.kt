package com.mpcorp.attendance.device.service

import com.mpcorp.attendance.audit.entity.ActorType
import com.mpcorp.attendance.audit.entity.AuditAction
import com.mpcorp.attendance.audit.service.AuditService
import com.mpcorp.attendance.common.crypto.Base64Utils
import com.mpcorp.attendance.common.crypto.PublicKeyParser
import com.mpcorp.attendance.common.crypto.SignatureVerifier
import com.mpcorp.attendance.common.exception.BusinessRuleException
import com.mpcorp.attendance.common.exception.ResourceNotFoundException
import com.mpcorp.attendance.device.dto.EnrollDeviceRequest
import com.mpcorp.attendance.device.dto.EnrollDeviceResponse
import com.mpcorp.attendance.device.dto.IssueEnrollmentCodeResponse
import com.mpcorp.attendance.device.entity.Device
import com.mpcorp.attendance.device.entity.DeviceStatus
import com.mpcorp.attendance.device.entity.EnrollmentCode
import com.mpcorp.attendance.device.mapper.DeviceMapper
import com.mpcorp.attendance.device.repository.DeviceRepository
import com.mpcorp.attendance.device.repository.EnrollmentCodeRepository
import com.mpcorp.attendance.device.security.EnrollmentProperties
import com.mpcorp.attendance.device.security.PairingCodeGenerator
import com.mpcorp.attendance.device.security.Sha256Hasher
import com.mpcorp.attendance.employee.repository.EmployeeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant

@Service
class EnrollmentService(
    private val employeeRepository: EmployeeRepository,
    private val deviceRepository: DeviceRepository,
    private val enrollmentCodeRepository: EnrollmentCodeRepository,
    private val pairingCodeGenerator: PairingCodeGenerator,
    private val sha256Hasher: Sha256Hasher,
    private val publicKeyParser: PublicKeyParser,
    private val signatureVerifier: SignatureVerifier,
    private val deviceMapper: DeviceMapper,
    private val properties: EnrollmentProperties,
    private val auditService: AuditService,
    private val clock: Clock,
) {

    /** Admin issues a one-time pairing code for an employee. */
    @Transactional
    fun issueCode(employeeId: Long, adminUsername: String?): IssueEnrollmentCodeResponse {
        val employee = employeeRepository.findById(employeeId)
            .orElseThrow { ResourceNotFoundException("Employee $employeeId not found") }
        if (!employee.active) {
            throw BusinessRuleException("Cannot issue a pairing code for an inactive employee")
        }

        val code = pairingCodeGenerator.generate()
        val entity = EnrollmentCode(
            employeeId = employeeId,
            codeHash = sha256Hasher.hash(code),
            expiresAt = Instant.now(clock).plusSeconds(properties.codeTtlSeconds),
            createdByAdmin = adminUsername,
        )
        enrollmentCodeRepository.save(entity)
        auditService.record(
            actorType = ActorType.ADMIN,
            action = AuditAction.ENROLL_CODE_ISSUED,
            actorId = adminUsername,
            targetType = "EMPLOYEE",
            targetId = employeeId.toString(),
        )
        return IssueEnrollmentCodeResponse(code = code, expiresAt = entity.expiresAt)
    }

    /**
     * Public device enrollment. Validates the one-time code, proves the device
     * holds the private key (signs the code), then registers the device as the
     * single ACTIVE device for the employee (revoking any previous one).
     */
    @Transactional
    fun enroll(request: EnrollDeviceRequest): EnrollDeviceResponse {
        val now = Instant.now(clock)
        val pairingCode = request.pairingCode.trim()

        val enrollmentCode = enrollmentCodeRepository.findByCodeHash(sha256Hasher.hash(pairingCode))
            ?: throw BusinessRuleException("Invalid or unknown pairing code")
        if (enrollmentCode.usedAt != null) throw BusinessRuleException("Pairing code already used")
        if (now.isAfter(enrollmentCode.expiresAt)) throw BusinessRuleException("Pairing code has expired")

        // Validates the key is a P-256 public key (throws InvalidPublicKeyException otherwise).
        val publicKey = publicKeyParser.parse(request.publicKey.trim())

        val proofSignature = try {
            Base64Utils.decode(request.proofSignature)
        } catch (ex: IllegalArgumentException) {
            throw BusinessRuleException("Proof signature is not valid Base64")
        }
        val proofValid = signatureVerifier.verify(
            publicKey,
            pairingCode.toByteArray(StandardCharsets.UTF_8),
            proofSignature,
        )
        if (!proofValid) throw BusinessRuleException("Proof-of-possession signature is invalid")

        // Enforce a single ACTIVE device per employee.
        deviceRepository.findByEmployeeIdAndStatus(enrollmentCode.employeeId, DeviceStatus.ACTIVE)
            .forEach { it.status = DeviceStatus.REVOKED }

        val device = Device(
            employeeId = enrollmentCode.employeeId,
            publicKey = request.publicKey.trim(),
            publicKeyFingerprint = sha256Hasher.hash(Base64Utils.decode(request.publicKey.trim())),
            platform = request.platform!!,
            deviceName = request.deviceName?.trim()?.takeIf { it.isNotEmpty() },
            status = DeviceStatus.ACTIVE,
            enrolledAt = now,
        )
        val saved = deviceRepository.save(device)
        enrollmentCode.usedAt = now

        // Loaded only on the success path, to echo the officer (cán bộ) identity back to the device.
        val employee = employeeRepository.findById(enrollmentCode.employeeId)
            .orElseThrow { ResourceNotFoundException("Employee ${enrollmentCode.employeeId} not found") }

        auditService.record(
            actorType = ActorType.DEVICE,
            action = AuditAction.DEVICE_ENROLLED,
            actorId = enrollmentCode.employeeId.toString(),
            targetType = "DEVICE",
            targetId = saved.id?.toString(),
        )
        return deviceMapper.toEnrollResponse(saved, employee)
    }
}
