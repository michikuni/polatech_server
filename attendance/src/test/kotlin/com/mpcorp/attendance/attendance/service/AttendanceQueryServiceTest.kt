package com.mpcorp.attendance.attendance.service

import com.mpcorp.attendance.attendance.entity.AttendanceEvent
import com.mpcorp.attendance.attendance.entity.AttendanceType
import com.mpcorp.attendance.attendance.mapper.AttendanceMapper
import com.mpcorp.attendance.attendance.repository.AttendanceEventRepository
import com.mpcorp.attendance.device.entity.Device
import com.mpcorp.attendance.device.entity.DevicePlatform
import com.mpcorp.attendance.device.repository.DeviceRepository
import com.mpcorp.attendance.employee.repository.EmployeeRepository
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AttendanceQueryServiceTest {

    private val repository = mock(AttendanceEventRepository::class.java)
    private val deviceRepository = mock(DeviceRepository::class.java)
    private val employeeRepository = mock(EmployeeRepository::class.java)
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")
    private val date = LocalDate.of(2026, 6, 29)
    private val clock = Clock.fixed(date.atTime(20, 0).atZone(zone).toInstant(), zone)
    private val service =
        AttendanceQueryService(repository, AttendanceMapper(), deviceRepository, employeeRepository, zone, clock)

    private val start = date.atStartOfDay(zone).toInstant()
    private val end = date.plusDays(1).atStartOfDay(zone).toInstant()

    private fun at(hour: Int, minute: Int): Instant =
        date.atTime(hour, minute).atZone(zone).toInstant()

    private fun event(type: AttendanceType, time: Instant) =
        AttendanceEvent(employeeId = 5L, deviceId = 3L, type = type, eventTime = time, challengeId = 1L)
            .apply { id = time.toEpochMilli() }

    private fun device() = Device(
        employeeId = 5L,
        publicKey = "pk",
        publicKeyFingerprint = "fp",
        platform = DevicePlatform.ANDROID,
        enrolledAt = at(8, 0),
    ).apply { id = 3L }

    private fun givenLastPunch(event: AttendanceEvent?) {
        given(deviceRepository.findById(3L)).willReturn(Optional.of(device()))
        given(repository.findFirstByEmployeeIdAndEventTimeGreaterThanEqualOrderByEventTimeDesc(5L, start))
            .willReturn(event)
    }

    @Test
    fun `dailySummary sums worked time across in-out pairs`() {
        val events = listOf(
            event(AttendanceType.CHECK_IN, at(9, 0)),
            event(AttendanceType.CHECK_OUT, at(12, 0)),
            event(AttendanceType.CHECK_IN, at(13, 0)),
            event(AttendanceType.CHECK_OUT, at(17, 0)),
        )
        given(repository.findDayEvents(5L, start, end)).willReturn(events)

        val summary = service.dailySummary(5L, date)

        assertEquals(7 * 3600L, summary.workedSeconds)
        assertEquals(at(9, 0), summary.firstCheckIn)
        assertEquals(at(17, 0), summary.lastCheckOut)
        assertEquals(2, summary.checkInCount)
        assertEquals(2, summary.checkOutCount)
        assertFalse(summary.openSession)
    }

    @Test
    fun `dailySummary flags an open session with no worked pairs`() {
        given(repository.findDayEvents(5L, start, end))
            .willReturn(listOf(event(AttendanceType.CHECK_IN, at(9, 0))))

        val summary = service.dailySummary(5L, date)

        assertEquals(0L, summary.workedSeconds)
        assertTrue(summary.openSession)
        assertEquals(at(9, 0), summary.firstCheckIn)
        assertEquals(null, summary.lastCheckOut)
    }

    @Test
    fun `dailySummary is empty when there are no events`() {
        given(repository.findDayEvents(5L, start, end)).willReturn(emptyList())

        val summary = service.dailySummary(5L, date)

        assertEquals(0L, summary.workedSeconds)
        assertFalse(summary.openSession)
        assertEquals(0, summary.checkInCount)
    }

    @Test
    fun `deviceHistory groups every punch per day, newest day first`() {
        given(deviceRepository.findById(3L)).willReturn(Optional.of(device()))

        // For a 30-day window anchored on the fixed clock (date), the service queries
        // events since the start of (date - 29 days) — stub that exact instant.
        val expectedFrom = date.minusDays(29).atStartOfDay(zone).toInstant()
        val yesterday830 = date.minusDays(1).atTime(8, 30).atZone(zone).toInstant()
        given(repository.findSince(5L, expectedFrom)).willReturn(
            listOf(
                event(AttendanceType.CHECK_IN, yesterday830),
                // Today: four punches — every one should appear, in order.
                event(AttendanceType.CHECK_IN, at(9, 0)),
                event(AttendanceType.CHECK_OUT, at(12, 0)),
                event(AttendanceType.CHECK_IN, at(13, 0)),
                event(AttendanceType.CHECK_OUT, at(17, 0)),
            ),
        )

        val history = service.deviceHistory(3L, 30)

        // Most-recent day first.
        assertEquals(2, history.size)
        assertEquals(date, history[0].date)
        assertEquals(
            listOf(
                AttendanceType.CHECK_IN to at(9, 0),
                AttendanceType.CHECK_OUT to at(12, 0),
                AttendanceType.CHECK_IN to at(13, 0),
                AttendanceType.CHECK_OUT to at(17, 0),
            ),
            history[0].punches.map { it.type to it.eventTime },
        )
        assertEquals(date.minusDays(1), history[1].date)
        assertEquals(1, history[1].punches.size)
        assertEquals(AttendanceType.CHECK_IN, history[1].punches.first().type)
        assertEquals(yesterday830, history[1].punches.first().eventTime)
    }

    @Test
    fun `deviceStatus reports an open session when the last punch is a check-in`() {
        givenLastPunch(event(AttendanceType.CHECK_IN, at(9, 0)))

        val status = service.deviceStatus(3L)

        assertTrue(status.openSession)
        assertEquals(AttendanceType.CHECK_IN, status.lastType)
        assertEquals(at(9, 0), status.lastEventTime)
    }

    @Test
    fun `deviceStatus reports closed when the last punch is a check-out`() {
        givenLastPunch(event(AttendanceType.CHECK_OUT, at(17, 0)))

        val status = service.deviceStatus(3L)

        assertFalse(status.openSession)
        assertEquals(AttendanceType.CHECK_OUT, status.lastType)
    }

    @Test
    fun `deviceStatus reports closed with no punch today`() {
        givenLastPunch(null)

        val status = service.deviceStatus(3L)

        assertFalse(status.openSession)
        assertEquals(null, status.lastType)
        assertEquals(null, status.lastEventTime)
    }
}
