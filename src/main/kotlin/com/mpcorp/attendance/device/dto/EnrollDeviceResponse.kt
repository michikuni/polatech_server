package com.mpcorp.attendance.device.dto

import com.mpcorp.attendance.device.entity.DeviceStatus

data class EnrollDeviceResponse(
    val deviceId: Long,
    val employeeId: Long,
    val status: DeviceStatus,
    // Officer (cán bộ) identity, so the paired device can show who it belongs to.
    val employeeCode: String,
    val fullName: String,
    val position: String,
    val rank: String,
)
