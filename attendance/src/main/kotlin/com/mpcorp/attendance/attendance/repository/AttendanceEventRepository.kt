package com.mpcorp.attendance.attendance.repository

import com.mpcorp.attendance.attendance.entity.AttendanceEvent
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface AttendanceEventRepository : JpaRepository<AttendanceEvent, Long> {

    @Query(
        """
        select a from AttendanceEvent a
        where (:employeeId is null or a.employeeId = :employeeId)
          and (:from is null or a.eventTime >= :from)
          and (:to is null or a.eventTime < :to)
        order by a.eventTime desc
        """,
    )
    fun search(
        @Param("employeeId") employeeId: Long?,
        @Param("from") from: Instant?,
        @Param("to") to: Instant?,
        pageable: Pageable,
    ): Page<AttendanceEvent>

    @Query(
        """
        select a from AttendanceEvent a
        where a.employeeId = :employeeId and a.eventTime >= :start and a.eventTime < :end
        order by a.eventTime asc
        """,
    )
    fun findDayEvents(
        @Param("employeeId") employeeId: Long,
        @Param("start") start: Instant,
        @Param("end") end: Instant,
    ): List<AttendanceEvent>

    @Query(
        """
        select a from AttendanceEvent a
        where a.employeeId = :employeeId and a.eventTime >= :from
        order by a.eventTime asc
        """,
    )
    fun findSince(
        @Param("employeeId") employeeId: Long,
        @Param("from") from: Instant,
    ): List<AttendanceEvent>
}
