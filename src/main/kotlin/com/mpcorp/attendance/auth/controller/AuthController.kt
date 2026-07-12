package com.mpcorp.attendance.auth.controller

import com.mpcorp.attendance.auth.dto.LoginRequest
import com.mpcorp.attendance.auth.dto.LoginResponse
import com.mpcorp.attendance.auth.service.AuthService
import com.mpcorp.attendance.common.response.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService,
) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<ApiResponse<LoginResponse>> {
        val response = authService.login(request)
        return ResponseEntity.ok(ApiResponse.ok(response))
    }
}
