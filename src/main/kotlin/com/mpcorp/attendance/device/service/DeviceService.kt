package com.mpcorp.attendance.device.service

import com.mpcorp.attendance.audit.entity.AuditAction
import com.mpcorp.attendance.audit.service.AuditService
import com.mpcorp.attendance.common.exception.ResourceNotFoundException
import com.mpcorp.attendance.common.response.PageResponse
import com.mpcorp.attendance.device.dto.DeviceResponse
import com.mpcorp.attendance.device.entity.DeviceStatus
import com.mpcorp.attendance.device.mapper.DeviceMapper
import com.mpcorp.attendance.device.repository.DeviceRepository
import com.mpcorp.attendance.employee.repository.EmployeeRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DeviceService(
    private val deviceRepository: DeviceRepository,
    private val deviceMapper: DeviceMapper,
    private val auditService: AuditService,
    private val employeeRepository: EmployeeRepository,
) {

    @Transactional(readOnly = true)
    fun list(employeeId: Long?, status: DeviceStatus?, pageable: Pageable): PageResponse<DeviceResponse> {
        val devices = deviceRepository.search(employeeId, status, pageable)
        val names = employeeNames(devices.content.map { it.employeeId })
        val page = devices.map { deviceMapper.toResponse(it, names[it.employeeId]) }
        return PageResponse.from(page)
    }

    @Transactional
    fun revoke(id: Long): DeviceResponse {
        val device = deviceRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Device $id not found") }
        device.status = DeviceStatus.REVOKED
        auditService.recordAdminAction(AuditAction.DEVICE_REVOKED, "DEVICE", id.toString())
        return deviceMapper.toResponse(device, employeeNames(listOf(device.employeeId))[device.employeeId])
    }

    /** Resolves employeeId -> fullName for a page of rows in one batched query. */
    private fun employeeNames(ids: Collection<Long>): Map<Long, String> =
        if (ids.isEmpty()) emptyMap()
        else employeeRepository.findAllById(ids.toSet()).associate { it.id!! to it.fullName }
}
