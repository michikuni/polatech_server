package com.mpcorp.attendance.audit.service

import com.mpcorp.attendance.audit.entity.ActorType
import com.mpcorp.attendance.audit.entity.AuditAction
import com.mpcorp.attendance.audit.entity.AuditLog
import com.mpcorp.attendance.audit.repository.AuditLogRepository
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class AuditServiceTest {

    private val repository = mock(AuditLogRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-06-29T03:00:00Z"), ZoneOffset.UTC)
    private val service = AuditService(repository, clock)

    @Test
    fun `record persists an audit entry with the given fields`() {
        val captor = ArgumentCaptor.forClass(AuditLog::class.java)

        service.record(
            actorType = ActorType.DEVICE,
            action = AuditAction.ATTENDANCE_RECORDED,
            actorId = "5",
            targetType = "ATTENDANCE",
            targetId = "100",
            detail = "CHECK_IN",
            ip = "10.0.0.5",
        )

        verify(repository).save(captor.capture())
        val saved = captor.value
        assertEquals(Instant.now(clock), saved.at)
        assertEquals(ActorType.DEVICE, saved.actorType)
        assertEquals(AuditAction.ATTENDANCE_RECORDED, saved.action)
        assertEquals("5", saved.actorId)
        assertEquals("ATTENDANCE", saved.targetType)
        assertEquals("100", saved.targetId)
        assertEquals("CHECK_IN", saved.detail)
        assertEquals("10.0.0.5", saved.ip)
    }

    @Test
    fun `recordAdminAction resolves the actor from the security context`() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken("admin", null, listOf(SimpleGrantedAuthority("ROLE_ADMIN")))
        try {
            val captor = ArgumentCaptor.forClass(AuditLog::class.java)

            service.recordAdminAction(AuditAction.EMPLOYEE_CREATED, "EMPLOYEE", "10")

            verify(repository).save(captor.capture())
            val saved = captor.value
            assertEquals(ActorType.ADMIN, saved.actorType)
            assertEquals("admin", saved.actorId)
            assertEquals(AuditAction.EMPLOYEE_CREATED, saved.action)
            assertEquals("10", saved.targetId)
        } finally {
            SecurityContextHolder.clearContext()
        }
    }
}
