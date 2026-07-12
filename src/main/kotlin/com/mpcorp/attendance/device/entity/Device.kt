package com.mpcorp.attendance.device.entity

import com.mpcorp.attendance.common.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.Instant

/**
 * A mobile device enrolled to an employee. Holds only the PUBLIC key; the
 * private key never leaves the device's Secure Enclave / Keystore.
 * At most one [DeviceStatus.ACTIVE] device per employee (enforced in service).
 */
@Entity
@Table(name = "device")
class Device(

    @Column(name = "employee_id", nullable = false)
    var employeeId: Long,

    @Column(name = "public_key", nullable = false, length = 512)
    var publicKey: String,

    @Column(name = "public_key_fingerprint", nullable = false, length = 64)
    var publicKeyFingerprint: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 16)
    var platform: DevicePlatform,

    @Column(name = "device_name", length = 200)
    var deviceName: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    var status: DeviceStatus = DeviceStatus.ACTIVE,

    @Column(name = "enrolled_at", nullable = false)
    var enrolledAt: Instant,

    @Column(name = "last_used_at")
    var lastUsedAt: Instant? = null,

) : BaseEntity()
