package com.mpcorp.attendance.employee.service

import com.mpcorp.attendance.attendance.repository.AttendanceEventRepository
import com.mpcorp.attendance.audit.service.AuditService
import com.mpcorp.attendance.common.exception.BusinessRuleException
import com.mpcorp.attendance.common.exception.ResourceNotFoundException
import com.mpcorp.attendance.device.repository.DeviceRepository
import com.mpcorp.attendance.device.repository.EnrollmentCodeRepository
import com.mpcorp.attendance.employee.dto.CreateEmployeeRequest
import com.mpcorp.attendance.employee.dto.UpdateEmployeeRequest
import com.mpcorp.attendance.employee.entity.Employee
import com.mpcorp.attendance.employee.mapper.EmployeeMapper
import com.mpcorp.attendance.employee.repository.EmployeeRepository
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class EmployeeServiceTest {

    private val repository = mock(EmployeeRepository::class.java)
    private val deviceRepository = mock(DeviceRepository::class.java)
    private val attendanceEventRepository = mock(AttendanceEventRepository::class.java)
    private val enrollmentCodeRepository = mock(EnrollmentCodeRepository::class.java)
    private val service = EmployeeService(
        repository,
        EmployeeMapper(),
        mock(AuditService::class.java),
        deviceRepository,
        attendanceEventRepository,
        enrollmentCodeRepository,
    )

    private fun sampleEntity(id: Long = 1L, code: String = "E001", active: Boolean = true) =
        Employee(employeeCode = code, fullName = "Alice", position = "Chuyên viên", rank = "Đại uý", active = active)
            .apply { this.id = id }

    @Test
    fun `create persists and returns the employee`() {
        given(repository.existsByEmployeeCode("E001")).willReturn(false)
        given(repository.save(any())).willAnswer { it.getArgument<Employee>(0).apply { id = 10L } }

        val result = service.create(CreateEmployeeRequest("E001", "Alice", "Chuyên viên", "Đại uý"))

        assertEquals(10L, result.id)
        assertEquals("E001", result.employeeCode)
        assertEquals("Alice", result.fullName)
    }

    @Test
    fun `create rejects a duplicate employee code`() {
        given(repository.existsByEmployeeCode("E001")).willReturn(true)
        assertFailsWith<BusinessRuleException> {
            service.create(CreateEmployeeRequest("E001", "Alice", "Chuyên viên", "Đại uý"))
        }
    }

    @Test
    fun `get returns the employee when found`() {
        given(repository.findById(1L)).willReturn(Optional.of(sampleEntity(1L)))
        assertEquals("E001", service.get(1L).employeeCode)
    }

    @Test
    fun `get throws when the employee is missing`() {
        given(repository.findById(99L)).willReturn(Optional.empty())
        assertFailsWith<ResourceNotFoundException> { service.get(99L) }
    }

    @Test
    fun `update changes the mutable fields`() {
        given(repository.findById(1L)).willReturn(Optional.of(sampleEntity(1L)))

        val result = service.update(1L, UpdateEmployeeRequest("Alice Smith", "Trưởng phòng", "Thiếu tá"))

        assertEquals("Alice Smith", result.fullName)
        assertEquals("Trưởng phòng", result.position)
        assertEquals("Thiếu tá", result.rank)
    }

    @Test
    fun `setActive deactivates the employee`() {
        given(repository.findById(1L)).willReturn(Optional.of(sampleEntity(1L, active = true)))
        assertFalse(service.setActive(1L, false).active)
    }

    @Test
    fun `delete removes an employee with no devices or attendance and clears pairing codes`() {
        given(repository.findById(1L)).willReturn(Optional.of(sampleEntity(1L)))
        given(deviceRepository.existsByEmployeeId(1L)).willReturn(false)
        given(attendanceEventRepository.existsByEmployeeId(1L)).willReturn(false)

        service.delete(1L)

        verify(enrollmentCodeRepository).deleteByEmployeeId(1L)
        verify(repository).delete(any())
    }

    @Test
    fun `delete is blocked when the employee still has a device`() {
        given(repository.findById(1L)).willReturn(Optional.of(sampleEntity(1L)))
        given(deviceRepository.existsByEmployeeId(1L)).willReturn(true)

        assertFailsWith<BusinessRuleException> { service.delete(1L) }
        verify(repository, never()).delete(any())
    }

    @Test
    fun `delete is blocked when the employee has attendance history`() {
        given(repository.findById(1L)).willReturn(Optional.of(sampleEntity(1L)))
        given(deviceRepository.existsByEmployeeId(1L)).willReturn(false)
        given(attendanceEventRepository.existsByEmployeeId(1L)).willReturn(true)

        assertFailsWith<BusinessRuleException> { service.delete(1L) }
        verify(repository, never()).delete(any())
    }

    @Test
    fun `delete throws when the employee is missing`() {
        given(repository.findById(99L)).willReturn(Optional.empty())
        assertFailsWith<ResourceNotFoundException> { service.delete(99L) }
    }

    @Test
    fun `list maps a page of employees`() {
        val pageable = PageRequest.of(0, 20)
        given(repository.search(null, null, pageable))
            .willReturn(PageImpl(listOf(sampleEntity(1L)), pageable, 1))

        val result = service.list(null, null, pageable)

        assertEquals(1, result.totalElements.toInt())
        assertEquals(1, result.items.size)
        assertEquals("E001", result.items[0].employeeCode)
    }
}
