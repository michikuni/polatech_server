package com.mpcorp.attendance.challenge.controller

import com.mpcorp.attendance.challenge.dto.ChallengeRequest
import com.mpcorp.attendance.challenge.dto.ChallengeResponse
import com.mpcorp.attendance.challenge.service.ChallengeService
import com.mpcorp.attendance.common.response.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Public endpoint: a device requests a challenge to sign. */
@RestController
@RequestMapping("/api/challenge")
class ChallengeController(
    private val challengeService: ChallengeService,
) {

    @PostMapping
    fun create(@Valid @RequestBody request: ChallengeRequest): ResponseEntity<ApiResponse<ChallengeResponse>> =
        ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(challengeService.issue(request.deviceId!!)))
}
