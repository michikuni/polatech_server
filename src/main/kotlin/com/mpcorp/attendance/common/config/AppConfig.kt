package com.mpcorp.attendance.common.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock
import java.time.ZoneId

@Configuration
class AppConfig {

    /** A single injectable clock so time-sensitive logic (JWT expiry, challenge
     *  TTL) is deterministic and testable. */
    @Bean
    fun clock(): Clock = Clock.systemUTC()

    /** The business time zone that defines an attendance "day" (day boundaries
     *  for daily summaries). Stored timestamps remain UTC. */
    @Bean
    fun businessZone(@Value("\${app.time-zone:Asia/Ho_Chi_Minh}") zoneId: String): ZoneId =
        ZoneId.of(zoneId)
}
