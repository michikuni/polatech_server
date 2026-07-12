package com.mpcorp.attendance.device.controller

import com.mpcorp.attendance.common.response.ApiResponse
import com.mpcorp.attendance.common.response.PageResponse
import com.mpcorp.attendance.device.dto.DeviceResponse
import com.mpcorp.attendance.device.entity.DeviceStatus
import com.mpcorp.attendance.device.service.DeviceService
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** Admin device management. */
@RestController
@RequestMapping("/api/admin/devices")
class DeviceController(
    private val deviceService: DeviceService,
) {

    @GetMapping
    fun list(
        @RequestParam(required = false) employeeId: Long?,
        @RequestParam(required = false) status: DeviceStatus?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): ResponseEntity<ApiResponse<PageResponse<DeviceResponse>>> =
        ResponseEntity.ok(ApiResponse.ok(deviceService.list(employeeId, status, pageable)))

    @PostMapping("/{id}/revoke")
    fun revoke(@PathVariable id: Long): ResponseEntity<ApiResponse<DeviceResponse>> =
        ResponseEntity.ok(ApiResponse.ok(deviceService.revoke(id)))
}
