package com.mpcorp.attendance.common.network

import org.springframework.stereotype.Component
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException

/** One address this server can be reached on, with the adapter it belongs to. */
data class LanAddress(
    val ip: String,
    /** Adapter name as the OS reports it, e.g. "Wi-Fi" or "VMware Network Adapter VMnet8". */
    val interfaceName: String,
)

/**
 * Discovers the IPv4 addresses a phone on the LAN can reach this server on, so
 * the admin portal can print a QR with a working address instead of the admin
 * having to know the machine's IP.
 *
 * Ordering matters: a typical Windows server also carries private IPs from
 * virtual adapters (VMware, Hyper-V, Docker), which no phone can reach. The best
 * candidate is the address the OS actually routes traffic out of, so that one is
 * ranked first — but every address is returned, because only the admin knows
 * which network the officers' phones are on.
 */
@Component
class LanAddressResolver {

    /** Reachable addresses of this host, best candidate first. Empty if the host
     *  has no usable interface (the portal then falls back to its own origin). */
    fun addresses(): List<LanAddress> = try {
        val candidates = NetworkInterface.networkInterfaces()
            .filter { iface -> runCatching { iface.isUp && !iface.isLoopback }.getOrDefault(false) }
            .flatMap { iface ->
                val name = iface.displayName ?: iface.name ?: ""
                iface.inetAddresses().map { Candidate(it, name) }
            }
            .toList()
        rank(candidates, outboundIp())
    } catch (ex: SocketException) {
        emptyList()
    }

    /**
     * The local address the OS would use to reach the outside world — on a normal
     * setup that is the physical adapter the phones share. Connecting a UDP socket
     * only fixes a route; **no packet is sent**, so this stays valid on an isolated
     * LAN with no Internet (it simply yields null if there is no route at all).
     */
    private fun outboundIp(): String? = try {
        DatagramSocket().use { socket ->
            socket.connect(InetAddress.getByName(ROUTE_PROBE_HOST), ROUTE_PROBE_PORT)
            (socket.localAddress as? Inet4Address)?.hostAddress
        }
    } catch (ex: Exception) {
        null
    }

    companion object {
        private const val ROUTE_PROBE_HOST = "8.8.8.8"
        private const val ROUTE_PROBE_PORT = 53

        /** Adapter-name fragments that mark a network a phone cannot join. */
        private val VIRTUAL_ADAPTER_HINTS = listOf(
            "vmware", "virtualbox", "vbox", "hyper-v", "vethernet", "docker",
            "wsl", "tap", "tunnel", "vpn", "loopback", "bluetooth",
        )

        data class Candidate(val address: InetAddress, val interfaceName: String)

        /** Dialable IPv4 addresses, most likely first. */
        fun rank(candidates: List<Candidate>, outboundIp: String?): List<LanAddress> = candidates
            .filter { it.address is Inet4Address }
            .filterNot {
                with(it.address) {
                    isLoopbackAddress || isLinkLocalAddress || isMulticastAddress || isAnyLocalAddress
                }
            }
            .sortedByDescending { score(it, outboundIp) }
            .mapNotNull { c -> c.address.hostAddress?.let { LanAddress(it, c.interfaceName) } }
            .distinctBy { it.ip }

        private fun score(candidate: Candidate, outboundIp: String?): Int {
            var score = 0
            if (candidate.address.hostAddress == outboundIp) score += 8 // the route out: the real NIC
            if (!isVirtualAdapter(candidate.interfaceName)) score += 4
            if (candidate.address.isSiteLocalAddress) score += 2        // a private range: LAN-shaped
            return score
        }

        fun isVirtualAdapter(interfaceName: String): Boolean {
            val name = interfaceName.lowercase()
            return VIRTUAL_ADAPTER_HINTS.any { name.contains(it) }
        }
    }
}
