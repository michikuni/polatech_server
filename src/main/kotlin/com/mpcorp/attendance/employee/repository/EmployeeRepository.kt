package com.mpcorp.attendance.employee.repository

import com.mpcorp.attendance.employee.entity.Employee
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface EmployeeRepository : JpaRepository<Employee, Long> {

    fun existsByEmployeeCode(employeeCode: String): Boolean

    /** Resolves a set of officer codes (case-insensitive) to their employees — used by the report export. */
    @Query("select e from Employee e where lower(e.employeeCode) in :codes")
    fun findByEmployeeCodeLowerIn(@Param("codes") codes: Collection<String>): List<Employee>

    @Query(
        """
        select e from Employee e
        where (:active is null or e.active = :active)
          and (
                :q is null
                or lower(e.fullName) like lower(concat('%', :q, '%'))
                or lower(e.employeeCode) like lower(concat('%', :q, '%'))
              )
        """,
    )
    fun search(
        @Param("active") active: Boolean?,
        @Param("q") q: String?,
        pageable: Pageable,
    ): Page<Employee>
}
