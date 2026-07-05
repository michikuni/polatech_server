package com.mpcorp.attendance.attendance.service

import com.mpcorp.attendance.attendance.entity.AttendanceEvent
import com.mpcorp.attendance.attendance.entity.AttendanceType
import com.mpcorp.attendance.attendance.repository.AttendanceEventRepository
import com.mpcorp.attendance.audit.entity.ActorType
import com.mpcorp.attendance.audit.entity.AuditAction
import com.mpcorp.attendance.audit.service.AuditService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.ZoneId

/**
 * Closes any attendance session still open at the end of the business day: for
 * every employee whose last punch today is a CHECK_IN, records a system-generated
 * CHECK_OUT. Kept separate from the scheduler so the sweep can be driven and
 * tested with an explicit "now".
 */
@Service
class AttendanceAutoCheckoutService(
    private val attendanceEventRepository: AttendanceEventRepository,
    private val auditService: AuditService,
    private val businessZone: ZoneId,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Records an auto check-out for each open session on the business day that
     * contains [now]. Returns how many were closed. [now] is used as the event
     * time of the generated check-outs (i.e. ~23:59 when run on schedule).
     */
    @Transactional
    fun closeOpenSessions(now: Instant): Int {
        val startOfDay = now.atZone(businessZone).toLocalDate().atStartOfDay(businessZone).toInstant()
        val endOfDay = startOfDay.atZone(businessZone).toLocalDate().plusDays(1).atStartOfDay(businessZone).toInstant()

        val openCheckIns = attendanceEventRepository.findBetween(startOfDay, endOfDay)
            .groupBy { it.employeeId }
            .mapNotNull { (_, events) -> events.maxByOrNull { it.eventTime } }
            .filter { it.type == AttendanceType.CHECK_IN }

        openCheckIns.forEach { checkIn -> autoCheckOut(checkIn, now) }

        if (openCheckIns.isNotEmpty()) {
            log.info("Auto check-out closed {} open session(s) for business day starting {}", openCheckIns.size, startOfDay)
        }
        return openCheckIns.size
    }

    private fun autoCheckOut(openCheckIn: AttendanceEvent, now: Instant) {
        val event = AttendanceEvent(
            employeeId = openCheckIn.employeeId,
            deviceId = openCheckIn.deviceId,
            type = AttendanceType.CHECK_OUT,
            eventTime = now,
            challengeId = null,
            sourceIp = null,
        )
        val saved = attendanceEventRepository.save(event)
        auditService.record(
            actorType = ActorType.SYSTEM,
            action = AuditAction.ATTENDANCE_AUTO_CHECKOUT,
            actorId = openCheckIn.employeeId.toString(),
            targetType = "ATTENDANCE",
            targetId = saved.id?.toString(),
            detail = "auto check-out for open check-in ${openCheckIn.id}",
        )
    }
}
