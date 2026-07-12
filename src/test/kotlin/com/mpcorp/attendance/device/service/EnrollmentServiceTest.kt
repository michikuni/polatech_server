package com.mpcorp.attendance.device.service

import com.mpcorp.attendance.audit.service.AuditService
import com.mpcorp.attendance.common.crypto.Base64Utils
import com.mpcorp.attendance.common.crypto.CryptoTestSupport
import com.mpcorp.attendance.common.crypto.InvalidPublicKeyException
import com.mpcorp.attendance.common.crypto.PublicKeyParser
import com.mpcorp.attendance.common.crypto.SignatureVerifier
import com.mpcorp.attendance.common.exception.BusinessRuleException
import com.mpcorp.attendance.common.exception.ResourceNotFoundException
import com.mpcorp.attendance.device.dto.EnrollDeviceRequest
import com.mpcorp.attendance.device.entity.Device
import com.mpcorp.attendance.device.entity.DevicePlatform
import com.mpcorp.attendance.device.entity.DeviceStatus
import com.mpcorp.attendance.device.entity.EnrollmentCode
import com.mpcorp.attendance.device.mapper.DeviceMapper
import com.mpcorp.attendance.device.repository.DeviceRepository
import com.mpcorp.attendance.device.repository.EnrollmentCodeRepository
import com.mpcorp.attendance.device.security.EnrollmentProperties
import com.mpcorp.attendance.device.security.PairingCodeGenerator
import com.mpcorp.attendance.device.security.Sha256Hasher
import com.mpcorp.attendance.employee.entity.Employee
import com.mpcorp.attendance.employee.repository.EmployeeRepository
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class EnrollmentServiceTest {

    private val employeeRepository = mock(EmployeeRepository::class.java)
    private val deviceRepository = mock(DeviceRepository::class.java)
    private val enrollmentCodeRepository = mock(EnrollmentCodeRepository::class.java)
    private val sha256Hasher = Sha256Hasher()
    private val properties = EnrollmentProperties(codeTtlSeconds = 600, codeLength = 10)
    private val pairingCodeGenerator = PairingCodeGenerator(properties)
    private val clock = Clock.fixed(Instant.parse("2026-06-28T00:00:00Z"), ZoneOffset.UTC)

    private val service = EnrollmentService(
        employeeRepository,
        deviceRepository,
        enrollmentCodeRepository,
        pairingCodeGenerator,
        sha256Hasher,
        PublicKeyParser(),
        SignatureVerifier(),
        DeviceMapper(),
        properties,
        mock(AuditService::class.java),
        clock,
    )

    private val pairingCode = "ABCDEFGHJK"

    private fun validCode(usedAt: Instant? = null, expiresAt: Instant = Instant.now(clock).plusSeconds(600)) =
        EnrollmentCode(employeeId = 5L, codeHash = sha256Hasher.hash(pairingCode), expiresAt = expiresAt, usedAt = usedAt)

    private fun enrollRequest(keyPair: KeyPair, signingKey: KeyPair = keyPair): EnrollDeviceRequest {
        val proof = CryptoTestSupport.sign(pairingCode.toByteArray(StandardCharsets.UTF_8), signingKey.private)
        return EnrollDeviceRequest(
            pairingCode = pairingCode,
            publicKey = Base64Utils.encode(keyPair.public.encoded),
            platform = DevicePlatform.IOS,
            deviceName = "iPhone",
            proofSignature = Base64Utils.encode(proof),
        )
    }

    // --- enroll ---

    @Test
    fun `enroll registers a device given a valid code and proof`() {
        val keyPair = CryptoTestSupport.generateEcKeyPair()
        val code = validCode()
        given(enrollmentCodeRepository.findByCodeHash(sha256Hasher.hash(pairingCode))).willReturn(code)
        given(deviceRepository.findByEmployeeIdAndStatus(5L, DeviceStatus.ACTIVE)).willReturn(emptyList())
        given(deviceRepository.save(any())).willAnswer { it.getArgument<Device>(0).apply { id = 7L } }
        given(employeeRepository.findById(5L)).willReturn(Optional.of(employee(active = true)))

        val result = service.enroll(enrollRequest(keyPair))

        assertEquals(7L, result.deviceId)
        assertEquals(5L, result.employeeId)
        assertEquals(DeviceStatus.ACTIVE, result.status)
        assertNotNull(code.usedAt, "Pairing code must be consumed after enrollment")
    }

    @Test
    fun `enroll revokes a previously active device`() {
        val keyPair = CryptoTestSupport.generateEcKeyPair()
        val previous = Device(
            employeeId = 5L,
            publicKey = "old",
            publicKeyFingerprint = "fp",
            platform = DevicePlatform.ANDROID,
            status = DeviceStatus.ACTIVE,
            enrolledAt = Instant.now(clock),
        ).apply { id = 1L }
        given(enrollmentCodeRepository.findByCodeHash(sha256Hasher.hash(pairingCode))).willReturn(validCode())
        given(deviceRepository.findByEmployeeIdAndStatus(5L, DeviceStatus.ACTIVE)).willReturn(listOf(previous))
        given(deviceRepository.save(any())).willAnswer { it.getArgument<Device>(0).apply { id = 8L } }
        given(employeeRepository.findById(5L)).willReturn(Optional.of(employee(active = true)))

        service.enroll(enrollRequest(keyPair))

        assertEquals(DeviceStatus.REVOKED, previous.status)
    }

    @Test
    fun `enroll rejects an unknown code`() {
        given(enrollmentCodeRepository.findByCodeHash(sha256Hasher.hash(pairingCode))).willReturn(null)
        assertFailsWith<BusinessRuleException> {
            service.enroll(enrollRequest(CryptoTestSupport.generateEcKeyPair()))
        }
    }

    @Test
    fun `enroll rejects an already used code`() {
        given(enrollmentCodeRepository.findByCodeHash(sha256Hasher.hash(pairingCode)))
            .willReturn(validCode(usedAt = Instant.now(clock)))
        assertFailsWith<BusinessRuleException> {
            service.enroll(enrollRequest(CryptoTestSupport.generateEcKeyPair()))
        }
    }

    @Test
    fun `enroll rejects an expired code`() {
        given(enrollmentCodeRepository.findByCodeHash(sha256Hasher.hash(pairingCode)))
            .willReturn(validCode(expiresAt = Instant.now(clock).minusSeconds(1)))
        assertFailsWith<BusinessRuleException> {
            service.enroll(enrollRequest(CryptoTestSupport.generateEcKeyPair()))
        }
    }

    @Test
    fun `enroll rejects a proof signed by a different key`() {
        val keyPair = CryptoTestSupport.generateEcKeyPair()
        val attacker = CryptoTestSupport.generateEcKeyPair()
        given(enrollmentCodeRepository.findByCodeHash(sha256Hasher.hash(pairingCode))).willReturn(validCode())
        assertFailsWith<BusinessRuleException> {
            service.enroll(enrollRequest(keyPair, signingKey = attacker))
        }
    }

    @Test
    fun `enroll rejects a malformed public key`() {
        given(enrollmentCodeRepository.findByCodeHash(sha256Hasher.hash(pairingCode))).willReturn(validCode())
        val request = EnrollDeviceRequest(
            pairingCode = pairingCode,
            publicKey = "!!not-a-key!!",
            platform = DevicePlatform.IOS,
            deviceName = null,
            proofSignature = Base64Utils.encode(byteArrayOf(1, 2, 3)),
        )
        assertFailsWith<InvalidPublicKeyException> { service.enroll(request) }
    }

    // --- issueCode ---

    @Test
    fun `issueCode returns a fresh code for an active employee`() {
        given(employeeRepository.findById(5L)).willReturn(Optional.of(employee(active = true)))
        given(enrollmentCodeRepository.save(any())).willAnswer { it.getArgument<EnrollmentCode>(0).apply { id = 1L } }

        val result = service.issueCode(5L, "admin")

        assertEquals(properties.codeLength, result.code.length)
        assertEquals(Instant.now(clock).plusSeconds(600), result.expiresAt)
    }

    @Test
    fun `issueCode rejects an inactive employee`() {
        given(employeeRepository.findById(5L)).willReturn(Optional.of(employee(active = false)))
        assertFailsWith<BusinessRuleException> { service.issueCode(5L, "admin") }
    }

    @Test
    fun `issueCode rejects a missing employee`() {
        given(employeeRepository.findById(9L)).willReturn(Optional.empty())
        assertFailsWith<ResourceNotFoundException> { service.issueCode(9L, "admin") }
    }

    private fun employee(active: Boolean) =
        Employee(employeeCode = "E001", fullName = "Alice", position = "Chuyên viên", rank = "Đại uý", active = active)
            .apply { id = 5L }
}
