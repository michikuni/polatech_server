package com.mpcorp.attendance.device.mapper

import com.mpcorp.attendance.device.dto.DeviceResponse
import com.mpcorp.attendance.device.dto.EnrollDeviceResponse
import com.mpcorp.attendance.device.entity.Device
import com.mpcorp.attendance.employee.entity.Employee
import org.springframework.stereotype.Component

@Component
class DeviceMapper {

    fun toResponse(device: Device, employeeName: String? = null): DeviceResponse = DeviceResponse(
        id = requireNotNull(device.id) { "Device id must not be null after persistence" },
        employeeId = device.employeeId,
        employeeName = employeeName,
        platform = device.platform,
        deviceName = device.deviceName,
        status = device.status,
        publicKeyFingerprint = device.publicKeyFingerprint,
        enrolledAt = device.enrolledAt,
        lastUsedAt = device.lastUsedAt,
        createdAt = device.createdAt,
    )

    fun toEnrollResponse(device: Device, employee: Employee): EnrollDeviceResponse = EnrollDeviceResponse(
        deviceId = requireNotNull(device.id) { "Device id must not be null after persistence" },
        employeeId = device.employeeId,
        status = device.status,
        employeeCode = employee.employeeCode,
        fullName = employee.fullName,
        position = employee.position,
        rank = employee.rank,
    )
}
