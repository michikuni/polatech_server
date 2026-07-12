package com.mpcorp.attendance.device.repository

import com.mpcorp.attendance.device.entity.EnrollmentCode
import org.springframework.data.jpa.repository.JpaRepository

interface EnrollmentCodeRepository : JpaRepository<EnrollmentCode, Long> {

    fun findByCodeHash(codeHash: String): EnrollmentCode?

    /** Removes an employee's one-time pairing codes — called when deleting the employee. */
    fun deleteByEmployeeId(employeeId: Long)
}
