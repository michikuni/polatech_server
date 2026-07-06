package com.mpcorp.attendance.device.repository

import com.mpcorp.attendance.device.entity.Device
import com.mpcorp.attendance.device.entity.DeviceStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface DeviceRepository : JpaRepository<Device, Long> {

    fun findByEmployeeIdAndStatus(employeeId: Long, status: DeviceStatus): List<Device>

    /** True if any device (of any status) is tied to this employee — guards employee deletion. */
    fun existsByEmployeeId(employeeId: Long): Boolean

    @Query(
        """
        select d from Device d
        where (:employeeId is null or d.employeeId = :employeeId)
          and (:status is null or d.status = :status)
        """,
    )
    fun search(
        @Param("employeeId") employeeId: Long?,
        @Param("status") status: DeviceStatus?,
        pageable: Pageable,
    ): Page<Device>
}
