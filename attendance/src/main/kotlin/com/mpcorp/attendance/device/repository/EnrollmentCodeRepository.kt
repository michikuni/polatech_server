package com.mpcorp.attendance.device.repository

import com.mpcorp.attendance.device.entity.EnrollmentCode
import org.springframework.data.jpa.repository.JpaRepository

interface EnrollmentCodeRepository : JpaRepository<EnrollmentCode, Long> {

    fun findByCodeHash(codeHash: String): EnrollmentCode?
}
