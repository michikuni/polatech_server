package com.mpcorp.attendance.common.network

import com.mpcorp.attendance.common.network.LanAddressResolver.Companion.Candidate
import org.junit.jupiter.api.Test
import java.net.InetAddress
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LanAddressResolverTest {

    private fun candidate(ip: String, iface: String) =
        Candidate(InetAddress.getByName(ip), iface)

    @Test
    fun `drops the addresses a phone cannot dial`() {
        val result = LanAddressResolver.rank(
            listOf(
                candidate("127.0.0.1", "Loopback"),
                candidate("169.254.3.4", "Ethernet"),
                candidate("0.0.0.0", "Ethernet"),
                candidate("192.168.1.10", "Wi-Fi"),
            ),
            outboundIp = null,
        )
        assertEquals(listOf(LanAddress("192.168.1.10", "Wi-Fi")), result)
    }

    @Test
    fun `puts the adapter the OS actually routes out of first`() {
        // The real machine this was built on: two virtual adapters carry private
        // IPs, while the NIC the phones share is the one holding the route out.
        val result = LanAddressResolver.rank(
            listOf(
                candidate("192.168.80.1", "VMware Network Adapter VMnet8"),
                candidate("192.168.126.1", "VMware Network Adapter VMnet1"),
                candidate("188.122.1.102", "Wi-Fi"),
            ),
            outboundIp = "188.122.1.102",
        )
        assertEquals("188.122.1.102", result.first().ip)
        assertEquals(3, result.size, "every address stays selectable by the admin")
    }

    @Test
    fun `prefers a real adapter over a virtual one when the route is unknown`() {
        val result = LanAddressResolver.rank(
            listOf(
                candidate("192.168.80.1", "VMware Network Adapter VMnet8"),
                candidate("192.168.1.10", "Ethernet"),
            ),
            outboundIp = null,
        )
        assertEquals("192.168.1.10", result.first().ip)
    }

    @Test
    fun `prefers a private address over a public one on equal adapters`() {
        val result = LanAddressResolver.rank(
            listOf(candidate("203.0.113.5", "Ethernet"), candidate("10.0.0.7", "Ethernet")),
            outboundIp = null,
        )
        assertEquals("10.0.0.7", result.first().ip)
    }

    @Test
    fun `drops duplicates and IPv6`() {
        val result = LanAddressResolver.rank(
            listOf(
                candidate("192.168.1.10", "Wi-Fi"),
                candidate("192.168.1.10", "Wi-Fi"),
                candidate("fe80::1", "Wi-Fi"),
                candidate("::1", "Loopback"),
            ),
            outboundIp = null,
        )
        assertEquals(listOf(LanAddress("192.168.1.10", "Wi-Fi")), result)
    }

    @Test
    fun `recognises virtual adapters by name`() {
        assertTrue(LanAddressResolver.isVirtualAdapter("VMware Network Adapter VMnet1"))
        assertTrue(LanAddressResolver.isVirtualAdapter("vEthernet (Default Switch)"))
        assertTrue(LanAddressResolver.isVirtualAdapter("Docker Desktop Bridge"))
        assertTrue(!LanAddressResolver.isVirtualAdapter("Wi-Fi"))
        assertTrue(!LanAddressResolver.isVirtualAdapter("Ethernet"))
    }

    @Test
    fun `real host lookup never throws and yields only IPv4 literals`() {
        val addresses = LanAddressResolver().addresses()
        assertTrue(
            addresses.all { it.ip.matches(Regex("""\d{1,3}(\.\d{1,3}){3}""")) },
            "got $addresses",
        )
    }
}
