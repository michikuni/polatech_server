package com.mpcorp.attendance.device.dto

import com.mpcorp.attendance.device.entity.DevicePlatform
import com.mpcorp.attendance.device.entity.DeviceStatus
import java.time.Instant

data class DeviceResponse(
    val id: Long,
    val employeeId: Long,
    val employeeName: String?,
    val platform: DevicePlatform,
    val deviceName: String?,
    val status: DeviceStatus,
    val publicKeyFingerprint: String,
    val enrolledAt: Instant,
    val lastUsedAt: Instant?,
    val createdAt: Instant?,
)
