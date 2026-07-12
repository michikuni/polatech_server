package com.mpcorp.attendance.auth.repository

import com.mpcorp.attendance.auth.entity.Admin
import org.springframework.data.jpa.repository.JpaRepository

interface AdminRepository : JpaRepository<Admin, Long> {

    fun findByUsername(username: String): Admin?
}
