package com.mpcorp.attendance.attendance.entity

import com.mpcorp.attendance.common.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.Instant

/** One attendance punch (check-in or check-out). Multiple per day are allowed. */
@Entity
@Table(name = "attendance_event")
class AttendanceEvent(

    @Column(name = "employee_id", nullable = false)
    var employeeId: Long,

    @Column(name = "device_id", nullable = false)
    var deviceId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    var type: AttendanceType,

    @Column(name = "event_time", nullable = false)
    var eventTime: Instant,

    @Column(name = "challenge_id", nullable = false)
    var challengeId: Long,

    @Column(name = "source_ip", length = 45)
    var sourceIp: String? = null,

) : BaseEntity()
