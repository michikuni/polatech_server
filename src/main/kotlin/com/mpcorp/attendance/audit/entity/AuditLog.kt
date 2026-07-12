package com.mpcorp.attendance.audit.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * Append-only audit record. Deliberately does NOT extend BaseEntity: audit rows
 * are never updated, so they carry their own [at] timestamp and no updatedAt.
 */
@Entity
@Table(name = "audit_log")
class AuditLog(

    @Column(name = "at", nullable = false)
    var at: Instant,

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 16)
    var actorType: ActorType,

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 40)
    var action: AuditAction,

    @Column(name = "actor_id", length = 100)
    var actorId: String? = null,

    @Column(name = "target_type", length = 40)
    var targetType: String? = null,

    @Column(name = "target_id", length = 64)
    var targetId: String? = null,

    @Column(name = "detail", length = 500)
    var detail: String? = null,

    @Column(name = "ip", length = 45)
    var ip: String? = null,

) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
}
