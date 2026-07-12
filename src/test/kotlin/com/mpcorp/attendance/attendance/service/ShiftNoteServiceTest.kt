package com.mpcorp.attendance.attendance.service

import com.mpcorp.attendance.attendance.dto.AttendanceNoteRequest
import com.mpcorp.attendance.attendance.entity.AttendanceEvent
import com.mpcorp.attendance.attendance.entity.AttendanceType
import com.mpcorp.attendance.attendance.repository.AttendanceEventRepository
import com.mpcorp.attendance.audit.service.AuditService
import com.mpcorp.attendance.common.exception.BusinessRuleException
import com.mpcorp.attendance.device.entity.Device
import com.mpcorp.attendance.device.entity.DevicePlatform
import com.mpcorp.attendance.device.repository.DeviceRepository
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import java.time.Instant
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ShiftNoteServiceTest {

    private val repository = mock(AttendanceEventRepository::class.java)
    private val deviceRepository = mock(DeviceRepository::class.java)
    private val service = ShiftNoteService(repository, deviceRepository, mock(AuditService::class.java))

    private val now = Instant.parse("2026-06-29T03:00:00Z")

    private fun event(
        type: AttendanceType = AttendanceType.CHECK_IN,
        employeeId: Long = 5L,
        note: String? = null,
    ) = AttendanceEvent(
        employeeId = employeeId,
        deviceId = 3L,
        type = type,
        eventTime = now,
        challengeId = 10L,
        note = note,
    ).apply { id = 100L }

    private fun device(employeeId: Long = 5L) = Device(
        employeeId = employeeId,
        publicKey = "pk",
        publicKeyFingerprint = "fp",
        platform = DevicePlatform.ANDROID,
        enrolledAt = now,
    ).apply { id = 3L }

    @Test
    fun `addNote stores the note on a check-in with no note yet`() {
        val event = event()
        given(repository.findById(100L)).willReturn(Optional.of(event))
        given(deviceRepository.findById(3L)).willReturn(Optional.of(device()))
        given(repository.save(any())).willAnswer { it.getArgument<AttendanceEvent>(0) }

        val result = service.addNote(AttendanceNoteRequest(3L, 100L, "  Bàn giao ca đêm  "), "10.0.0.5")

        assertEquals(100L, result.eventId)
        assertEquals("Bàn giao ca đêm", result.note) // trimmed
        assertEquals("Bàn giao ca đêm", event.note)
    }

    @Test
    fun `addNote rejects a second note (write-once) and does not save`() {
        given(repository.findById(100L)).willReturn(Optional.of(event(note = "existing")))
        given(deviceRepository.findById(3L)).willReturn(Optional.of(device()))

        assertFailsWith<BusinessRuleException> {
            service.addNote(AttendanceNoteRequest(3L, 100L, "another"), null)
        }
        verify(repository, never()).save(any())
    }

    @Test
    fun `addNote rejects a note on a check-out`() {
        given(repository.findById(100L)).willReturn(Optional.of(event(type = AttendanceType.CHECK_OUT)))
        given(deviceRepository.findById(3L)).willReturn(Optional.of(device()))

        assertFailsWith<BusinessRuleException> {
            service.addNote(AttendanceNoteRequest(3L, 100L, "note"), null)
        }
        verify(repository, never()).save(any())
    }

    @Test
    fun `addNote rejects an event owned by a different employee`() {
        given(repository.findById(100L)).willReturn(Optional.of(event(employeeId = 7L)))
        given(deviceRepository.findById(3L)).willReturn(Optional.of(device(employeeId = 5L)))

        assertFailsWith<BusinessRuleException> {
            service.addNote(AttendanceNoteRequest(3L, 100L, "note"), null)
        }
        verify(repository, never()).save(any())
    }
}
