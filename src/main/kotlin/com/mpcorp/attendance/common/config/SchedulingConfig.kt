package com.mpcorp.attendance.common.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/** Enables Spring's @Scheduled support (used by the daily auto check-out job). */
@Configuration
@EnableScheduling
class SchedulingConfig
