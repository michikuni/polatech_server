package com.mpcorp.attendance.device.security

import org.springframework.boot.context.properties.ConfigurationProperties

/** Enrollment tuning, bound from `app.enrollment.*`. */
@ConfigurationProperties(prefix = "app.enrollment")
data class EnrollmentProperties(
    val codeTtlSeconds: Long = 600,
    val codeLength: Int = 10,
)
