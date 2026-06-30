package com.mpcorp.attendance.device.dto

import java.time.Instant

/** Returned to the admin once; [code] is the plaintext pairing code to hand to the employee. */
data class IssueEnrollmentCodeResponse(
    val code: String,
    val expiresAt: Instant,
)
