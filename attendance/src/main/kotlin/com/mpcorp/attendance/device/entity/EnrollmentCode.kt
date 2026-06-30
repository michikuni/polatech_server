package com.mpcorp.attendance.device.entity

import com.mpcorp.attendance.common.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant

/**
 * One-time pairing code issued by an admin to let an employee enroll a device.
 * Only the SHA-256 hash of the code is stored, never the plaintext.
 */
@Entity
@Table(name = "enrollment_code")
class EnrollmentCode(

    @Column(name = "employee_id", nullable = false)
    var employeeId: Long,

    @Column(name = "code_hash", nullable = false, length = 64)
    var codeHash: String,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @Column(name = "used_at")
    var usedAt: Instant? = null,

    @Column(name = "created_by_admin", length = 100)
    var createdByAdmin: String? = null,

) : BaseEntity()
