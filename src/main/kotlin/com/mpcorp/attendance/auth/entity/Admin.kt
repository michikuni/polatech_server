package com.mpcorp.attendance.auth.entity

import com.mpcorp.attendance.common.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "admin")
class Admin(

    @Column(name = "username", nullable = false, length = 100, unique = true)
    var username: String,

    @Column(name = "password_hash", nullable = false, length = 100)
    var passwordHash: String,

    @Column(name = "display_name", length = 200)
    var displayName: String? = null,

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true,

) : BaseEntity()
