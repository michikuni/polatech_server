package com.mpcorp.attendance.audit.service

import com.mpcorp.attendance.audit.entity.ActorType
import com.mpcorp.attendance.audit.entity.AuditAction
import com.mpcorp.attendance.audit.entity.AuditLog
import com.mpcorp.attendance.audit.mapper.AuditMapper
import com.mpcorp.attendance.audit.repository.AuditLogRepository
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class AuditQueryServiceTest {

    private val repository = mock(AuditLogRepository::class.java)
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")
    private val service = AuditQueryService(repository, AuditMapper(), zone)

    @Test
    fun `list maps a page of audit entries`() {
        val pageable = PageRequest.of(0, 20)
        val entry = AuditLog(
            at = Instant.parse("2026-06-29T03:00:00Z"),
            actorType = ActorType.ADMIN,
            action = AuditAction.ADMIN_LOGIN,
            actorId = "admin",
        ).apply { id = 1L }
        given(repository.search(null, null, null, null, pageable))
            .willReturn(PageImpl(listOf(entry), pageable, 1))

        val result = service.list(null, null, null, null, pageable)

        assertEquals(1, result.items.size)
        assertEquals(AuditAction.ADMIN_LOGIN, result.items[0].action)
        assertEquals("admin", result.items[0].actorId)
    }
}
