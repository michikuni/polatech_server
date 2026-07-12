package com.mpcorp.attendance.common.network

/**
 * What the admin portal needs to hand a working server address to the devices:
 * the reachable base URLs of this server, best candidate first, each labelled
 * with the network adapter it belongs to so the admin can tell a real NIC from a
 * virtual one.
 */
data class ServerInfoResponse(
    val port: Int,
    val addresses: List<ServerAddress>,
)

data class ServerAddress(
    /** e.g. `http://192.168.1.10:8080`. */
    val baseUrl: String,
    /** e.g. `Wi-Fi`. */
    val interfaceName: String,
)
