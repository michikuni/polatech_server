package com.mpcorp.attendance.auth.dto

data class LoginResponse(
    val accessToken: String,
    val expiresInSeconds: Long,
    val tokenType: String = "Bearer",
)
