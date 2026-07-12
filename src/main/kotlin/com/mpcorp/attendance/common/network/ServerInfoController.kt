package com.mpcorp.attendance.common.network

import com.mpcorp.attendance.common.response.ApiResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Admin-only: the LAN addresses this server answers on (used to build the QRs). */
@RestController
@RequestMapping("/api/admin/server-info")
class ServerInfoController(
    private val lanAddressResolver: LanAddressResolver,
    @Value("\${server.port:8080}") private val port: Int,
) {

    @GetMapping
    fun info(): ResponseEntity<ApiResponse<ServerInfoResponse>> {
        val addresses = lanAddressResolver.addresses().map {
            ServerAddress(baseUrl = "http://${it.ip}:$port", interfaceName = it.interfaceName)
        }
        return ResponseEntity.ok(ApiResponse.ok(ServerInfoResponse(port = port, addresses = addresses)))
    }
}
