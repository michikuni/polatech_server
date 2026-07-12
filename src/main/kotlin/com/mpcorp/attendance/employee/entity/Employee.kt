package com.mpcorp.attendance.employee.entity

import com.mpcorp.attendance.common.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "employee")
class Employee(

    @Column(name = "employee_code", nullable = false, length = 50, unique = true)
    var employeeCode: String,

    @Column(name = "full_name", nullable = false, length = 200)
    var fullName: String,

    /** Chức vụ (e.g. "Trưởng phòng"). */
    @Column(name = "position", nullable = false, length = 200)
    var position: String,

    /** Cấp bậc (e.g. "Đại uý"). Column is `officer_rank` because RANK is a reserved word in MySQL. */
    @Column(name = "officer_rank", nullable = false, length = 200)
    var rank: String,

    @Column(name = "active", nullable = false)
    var active: Boolean = true,

) : BaseEntity()
