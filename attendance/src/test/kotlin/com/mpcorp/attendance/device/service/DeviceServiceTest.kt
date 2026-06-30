package com.mpcorp.attendance.device.service

import com.mpcorp.attendance.audit.service.AuditService
import com.mpcorp.attendance.common.exception.ResourceNotFoundException
import com.mpcorp.attendance.device.entity.Device
import com.mpcorp.attendance.device.entity.DevicePlatform
import com.mpcorp.attendance.device.entity.DeviceStatus
import com.mpcorp.attendance.device.mapper.DeviceMapper
import com.mpcorp.attendance.device.repository.DeviceRepository
import com.mpcorp.attendance.employee.entity.Employee
import com.mpcorp.attendance.employee.repository.EmployeeRepository
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Instant
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeviceServiceTest {

    private val deviceRepository = mock(DeviceRepository::class.java)
    private val employeeRepository = mock(EmployeeRepository::class.java)
    private val service =
        DeviceService(deviceRepository, DeviceMapper(), mock(AuditService::class.java), employeeRepository)

    private fun sampleDevice(id: Long = 3L, status: DeviceStatus = DeviceStatus.ACTIVE) = Device(
        employeeId = 5L,
        publicKey = "key",
        publicKeyFingerprint = "fp",
        platform = DevicePlatform.IOS,
        status = status,
        enrolledAt = Instant.parse("2026-06-28T00:00:00Z"),
    ).apply { this.id = id }

    @Test
    fun `revoke marks the device revoked`() {
        given(deviceRepository.findById(3L)).willReturn(Optional.of(sampleDevice()))
        assertEquals(DeviceStatus.REVOKED, service.revoke(3L).status)
    }

    @Test
    fun `revoke throws when the device is missing`() {
        given(deviceRepository.findById(99L)).willReturn(Optional.empty())
        assertFailsWith<ResourceNotFoundException> { service.revoke(99L) }
    }

    @Test
    fun `list maps a page of devices with employee names`() {
        val pageable = PageRequest.of(0, 20)
        given(deviceRepository.search(5L, null, pageable))
            .willReturn(PageImpl(listOf(sampleDevice()), pageable, 1))
        given(employeeRepository.findAllById(setOf(5L))).willReturn(
            listOf(Employee(employeeCode = "E001", fullName = "Alice", position = "Chuyên viên", rank = "Đại uý").apply { id = 5L }),
        )

        val result = service.list(5L, null, pageable)

        assertEquals(1, result.items.size)
        assertEquals(5L, result.items[0].employeeId)
        assertEquals("Alice", result.items[0].employeeName)
    }
}
