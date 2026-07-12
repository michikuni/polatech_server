package com.mpcorp.attendance.challenge.entity

import com.mpcorp.attendance.common.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant

/**
 * A single-use challenge (32-byte nonce, Base64) bound to one device. Validity
 * is derived, not stored: usable while [consumedAt] is null and now <= [expiresAt].
 */
@Entity
@Table(name = "challenge")
class Challenge(

    @Column(name = "device_id", nullable = false)
    var deviceId: Long,

    @Column(name = "challenge", nullable = false, length = 64)
    var challenge: String,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @Column(name = "consumed_at")
    var consumedAt: Instant? = null,

) : BaseEntity()
