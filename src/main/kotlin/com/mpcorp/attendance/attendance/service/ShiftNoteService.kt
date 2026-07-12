package com.mpcorp.attendance.attendance.service

import com.mpcorp.attendance.attendance.dto.AttendanceNoteRequest
import com.mpcorp.attendance.attendance.dto.AttendanceNoteResponse
import com.mpcorp.attendance.attendance.entity.AttendanceType
import com.mpcorp.attendance.attendance.repository.AttendanceEventRepository
import com.mpcorp.attendance.audit.entity.ActorType
import com.mpcorp.attendance.audit.entity.AuditAction
import com.mpcorp.attendance.audit.service.AuditService
import com.mpcorp.attendance.common.exception.BusinessRuleException
import com.mpcorp.attendance.common.exception.ResourceNotFoundException
import com.mpcorp.attendance.device.repository.DeviceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Attaches the one-time "shift handover" note (Thông tin tiếp nhận ca trực) to
 * the check-in that opens a session. Write-once: once set, the note is immutable.
 */
@Service
class ShiftNoteService(
    private val attendanceEventRepository: AttendanceEventRepository,
    private val deviceRepository: DeviceRepository,
    private val auditService: AuditService,
) {

    @Transactional
    fun addNote(request: AttendanceNoteRequest, sourceIp: String?): AttendanceNoteResponse {
        val event = attendanceEventRepository.findById(request.eventId!!)
            .orElseThrow { ResourceNotFoundException("Attendance event ${request.eventId} not found") }
        val device = deviceRepository.findById(request.deviceId!!)
            .orElseThrow { ResourceNotFoundException("Device ${request.deviceId} not found") }

        // Device-scoped ownership: a device may only annotate its own officer's punches.
        if (event.employeeId != device.employeeId) {
            throw BusinessRuleException("Sự kiện không thuộc thiết bị này")
        }
        // The note is the shift-takeover record, so it belongs on the check-in.
        if (event.type != AttendanceType.CHECK_IN) {
            throw BusinessRuleException("Chỉ có thể ghi chú cho lượt vào ca")
        }
        // Write-once: an existing note can be viewed but never changed.
        if (!event.note.isNullOrBlank()) {
            throw BusinessRuleException("Ghi chú đã tồn tại và không thể sửa")
        }

        val note = request.note!!.trim()
        event.note = note
        val saved = attendanceEventRepository.save(event)
        auditService.record(
            actorType = ActorType.DEVICE,
            action = AuditAction.ATTENDANCE_NOTE_ADDED,
            actorId = device.employeeId.toString(),
            targetType = "ATTENDANCE",
            targetId = saved.id?.toString(),
            detail = "shift note added",
            ip = sourceIp,
        )
        return AttendanceNoteResponse(eventId = saved.id!!, note = note)
    }
}
