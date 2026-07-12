package com.mpcorp.attendance.attendance.scheduler

import com.mpcorp.attendance.attendance.service.AttendanceAutoCheckoutService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant

/**
 * Fires the daily auto check-out at 23:59 in the business time zone. The cron and
 * zone are configurable; the actual closing logic lives in
 * [AttendanceAutoCheckoutService] so it can be unit-tested without the scheduler.
 */
@Component
class AttendanceScheduler(
    private val autoCheckoutService: AttendanceAutoCheckoutService,
    private val clock: Clock,
) {

    @Scheduled(
        cron = "\${app.auto-checkout.cron:0 59 23 * * *}",
        zone = "\${app.time-zone:Asia/Ho_Chi_Minh}",
    )
    fun autoCheckout() {
        autoCheckoutService.closeOpenSessions(Instant.now(clock))
    }
}
