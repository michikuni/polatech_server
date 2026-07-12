package com.mpcorp.attendance.device.dto

import com.mpcorp.attendance.device.entity.DevicePlatform
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class EnrollDeviceRequest(

    @field:NotBlank
    val pairingCode: String,

    @field:NotBlank
    val publicKey: String,

    @field:NotNull
    val platform: DevicePlatform?,

    @field:Size(max = 200)
    val deviceName: String? = null,

    @field:NotBlank
    val proofSignature: String,
)
