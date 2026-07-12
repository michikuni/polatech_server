package com.mpcorp.attendance.attendance.service

import com.mpcorp.attendance.attendance.dto.AttendanceEventResponse
import com.mpcorp.attendance.attendance.dto.AttendanceStatusResponse
import com.mpcorp.attendance.attendance.dto.DailyHistoryResponse
import com.mpcorp.attendance.attendance.dto.DailyPunchResponse
import com.mpcorp.attendance.attendance.dto.DailySummaryResponse
import com.mpcorp.attendance.attendance.entity.AttendanceEvent
import com.mpcorp.attendance.attendance.entity.AttendanceType
import com.mpcorp.attendance.attendance.mapper.AttendanceMapper
import com.mpcorp.attendance.attendance.repository.AttendanceEventRepository
import com.mpcorp.attendance.common.exception.ResourceNotFoundException
import com.mpcorp.attendance.common.response.PageResponse
import com.mpcorp.attendance.device.repository.DeviceRepository
import com.mpcorp.attendance.employee.repository.EmployeeRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Service
class AttendanceQueryService(
    private val attendanceEventRepository: AttendanceEventRepository,
    private val attendanceMapper: AttendanceMapper,
    private val deviceRepository: DeviceRepository,
    private val employeeRepository: EmployeeRepository,
    private val businessZone: ZoneId,
    private val clock: Clock,
) {

    @Transactional(readOnly = true)
    fun list(
        employeeId: Long?,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        pageable: Pageable,
    ): PageResponse<AttendanceEventResponse> {
        val from = fromDate?.atStartOfDay(businessZone)?.toInstant()
        val to = toDate?.plusDays(1)?.atStartOfDay(businessZone)?.toInstant()
        val events = attendanceEventRepository.search(employeeId, from, to, pageable)
        val names = employeeNames(events.content.map { it.employeeId })
        val page = events.map { attendanceMapper.toResponse(it, names[it.employeeId]) }
        return PageResponse.from(page)
    }

    /** Resolves employeeId -> fullName for a page of rows in one batched query. */
    private fun employeeNames(ids: Collection<Long>): Map<Long, String> =
        if (ids.isEmpty()) emptyMap()
        else employeeRepository.findAllById(ids.toSet()).associate { it.id!! to it.fullName }

    /**
     * Per-day attendance for the employee owning [deviceId], most recent day
     * first, over the last [days] days. Each day carries every punch of that day
     * in chronological order. Days with no events are omitted.
     */
    @Transactional(readOnly = true)
    fun deviceHistory(deviceId: Long, days: Int): List<DailyHistoryResponse> {
        val device = deviceRepository.findById(deviceId)
            .orElseThrow { ResourceNotFoundException("Device $deviceId not found") }
        val window = days.coerceIn(1, 366)
        val from = LocalDate.now(clock.withZone(businessZone))
            .minusDays((window - 1).toLong())
            .atStartOfDay(businessZone)
            .toInstant()

        return attendanceEventRepository.findSince(device.employeeId, from)
            .groupBy { it.eventTime.atZone(businessZone).toLocalDate() }
            .map { (date, dayEvents) ->
                DailyHistoryResponse(
                    date = date,
                    punches = dayEvents
                        .sortedBy { it.eventTime }
                        .map {
                            DailyPunchResponse(
                                id = it.id!!,
                                type = it.type,
                                eventTime = it.eventTime,
                                note = it.note,
                            )
                        },
                )
            }
            .sortedByDescending { it.date }
    }

    /**
     * The employee's current attendance state for today, derived from the last
     * punch. Mirrors the alternation rule enforced on record so the device can
     * grey out the button that would be rejected.
     */
    @Transactional(readOnly = true)
    fun deviceStatus(deviceId: Long): AttendanceStatusResponse {
        val device = deviceRepository.findById(deviceId)
            .orElseThrow { ResourceNotFoundException("Device $deviceId not found") }
        val startOfDay = LocalDate.now(clock.withZone(businessZone))
            .atStartOfDay(businessZone)
            .toInstant()
        val last = attendanceEventRepository
            .findFirstByEmployeeIdAndEventTimeGreaterThanEqualOrderByEventTimeDesc(device.employeeId, startOfDay)
        return AttendanceStatusResponse(
            openSession = last?.type == AttendanceType.CHECK_IN,
            lastType = last?.type,
            lastEventTime = last?.eventTime,
        )
    }

    @Transactional(readOnly = true)
    fun dailySummary(employeeId: Long, date: LocalDate): DailySummaryResponse {
        val start = date.atStartOfDay(businessZone).toInstant()
        val end = date.plusDays(1).atStartOfDay(businessZone).toInstant()
        val events = attendanceEventRepository.findDayEvents(employeeId, start, end)
        return summarize(employeeId, date, events)
    }

    /** Pairs check-ins with the next check-out in chronological order and sums the worked time. */
    private fun summarize(employeeId: Long, date: LocalDate, events: List<AttendanceEvent>): DailySummaryResponse {
        var workedSeconds = 0L
        var openCheckIn: Instant? = null
        var firstCheckIn: Instant? = null
        var lastCheckOut: Instant? = null
        var checkInCount = 0
        var checkOutCount = 0

        for (event in events) {
            when (event.type) {
                AttendanceType.CHECK_IN -> {
                    checkInCount++
                    if (firstCheckIn == null) firstCheckIn = event.eventTime
                    if (openCheckIn == null) openCheckIn = event.eventTime
                }

                AttendanceType.CHECK_OUT -> {
                    checkOutCount++
                    lastCheckOut = event.eventTime
                    val open = openCheckIn
                    if (open != null) {
                        workedSeconds += Duration.between(open, event.eventTime).seconds
                        openCheckIn = null
                    }
                }
            }
        }

        return DailySummaryResponse(
            employeeId = employeeId,
            date = date,
            workedSeconds = workedSeconds,
            firstCheckIn = firstCheckIn,
            lastCheckOut = lastCheckOut,
            checkInCount = checkInCount,
            checkOutCount = checkOutCount,
            openSession = openCheckIn != null,
        )
    }
}
