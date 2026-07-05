package com.mpcorp.attendance.attendance.service

import com.mpcorp.attendance.attendance.entity.AttendanceEvent
import com.mpcorp.attendance.attendance.entity.AttendanceType
import com.mpcorp.attendance.attendance.repository.AttendanceEventRepository
import com.mpcorp.attendance.audit.service.AuditService
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AttendanceAutoCheckoutServiceTest {

    private val repository = mock(AttendanceEventRepository::class.java)
    private val auditService = mock(AuditService::class.java)
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")
    private val date = LocalDate.of(2026, 6, 29)
    private val now = date.atTime(23, 59).atZone(zone).toInstant()
    private val service = AttendanceAutoCheckoutService(repository, auditService, zone)

    private val start = date.atStartOfDay(zone).toInstant()
    private val end = date.plusDays(1).atStartOfDay(zone).toInstant()

    private fun at(hour: Int, minute: Int): Instant =
        date.atTime(hour, minute).atZone(zone).toInstant()

    private fun event(employeeId: Long, deviceId: Long, type: AttendanceType, time: Instant) =
        AttendanceEvent(employeeId = employeeId, deviceId = deviceId, type = type, eventTime = time, challengeId = 1L)
            .apply { id = employeeId }

    /** Records every saved event so we can assert what the sweep generated. */
    private fun captureSaves(): MutableList<AttendanceEvent> {
        val saved = mutableListOf<AttendanceEvent>()
        given(repository.save(any())).willAnswer {
            val e = it.getArgument<AttendanceEvent>(0)
            e.id = 500L + saved.size
            saved.add(e)
            e
        }
        return saved
    }

    @Test
    fun `closeOpenSessions checks out only employees whose last punch is a check-in`() {
        given(repository.findBetween(start, end)).willReturn(
            listOf(
                // employee 5: still checked in -> should be auto closed
                event(5L, 50L, AttendanceType.CHECK_IN, at(9, 0)),
                // employee 6: already checked out -> left alone
                event(6L, 60L, AttendanceType.CHECK_IN, at(9, 0)),
                event(6L, 60L, AttendanceType.CHECK_OUT, at(17, 0)),
                // employee 7: second session still open -> should be auto closed
                event(7L, 70L, AttendanceType.CHECK_IN, at(8, 0)),
                event(7L, 70L, AttendanceType.CHECK_OUT, at(12, 0)),
                event(7L, 70L, AttendanceType.CHECK_IN, at(13, 0)),
            ),
        )
        val saved = captureSaves()

        val closed = service.closeOpenSessions(now)

        assertEquals(2, closed)
        assertEquals(2, saved.size)
        assertTrue(saved.all { it.type == AttendanceType.CHECK_OUT })
        assertTrue(saved.all { it.eventTime == now })
        assertTrue(saved.all { it.challengeId == null })
        assertTrue(saved.all { it.sourceIp == null })
        assertEquals(setOf(5L, 7L), saved.map { it.employeeId }.toSet())
        // The auto check-out reuses the device of the open check-in.
        assertEquals(setOf(50L, 70L), saved.map { it.deviceId }.toSet())
    }

    @Test
    fun `closeOpenSessions does nothing when every session is already closed`() {
        given(repository.findBetween(start, end)).willReturn(
            listOf(
                event(5L, 50L, AttendanceType.CHECK_IN, at(9, 0)),
                event(5L, 50L, AttendanceType.CHECK_OUT, at(17, 0)),
            ),
        )

        val closed = service.closeOpenSessions(now)

        assertEquals(0, closed)
        verify(repository, never()).save(any())
    }
}
